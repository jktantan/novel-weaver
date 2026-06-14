package com.novelweaver.service;

import com.arcadedb.remote.RemoteDatabase;
import com.novelweaver.config.ArcadeDBManager;
import com.novelweaver.model.CharacterRelationship;
import com.novelweaver.repository.CharacterRelationshipRepository;
import com.novelweaver.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GraphService {

    private static final Logger log = LoggerFactory.getLogger(GraphService.class);
    private static final int MAX_DEPTH = 10;

    private final CharacterRelationshipRepository rels;
    private final ProjectRepository projects;
    private final ArcadeDBManager arcadeDB;

    public GraphService(CharacterRelationshipRepository rels, ProjectRepository projects,
                        ArcadeDBManager arcadeDB) {
        this.rels = rels;
        this.projects = projects;
        this.arcadeDB = arcadeDB;
    }

    @McpTool(name = "graph_query", description = "查询角色关系子图 | CN 查询关系子图 / JP 関係サブグラフ照会 / EN Query relationship subgraph")
    public GraphQueryResult query(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "角色名", required = true) String entityName,
            @McpToolParam(description = "关系深度（默认2）", required = false) Integer depth) {

        var proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        int d = Math.max(1, Math.min(depth != null ? depth : 2, MAX_DEPTH));

        try (RemoteDatabase db = arcadeDB.open(projectId)) {
            String cypher = "MATCH (c:Character {name: $name})-[r:RELATED_TO*1..%d]-(other:Character) " +
                    "RETURN DISTINCT c.name AS from, other.name AS to, " +
                    "type(r[0]) AS relType, r[0].trustLevel AS trustLevel, r[0].note AS note LIMIT 100";
            var result = db.query("cypher", String.format(cypher, d), Map.of("name", entityName));
            List<GraphEdge> edges = new ArrayList<>();
            while (result.hasNext()) {
                var row = result.next();
                edges.add(new GraphEdge(
                        row.getProperty("from").toString(),
                        row.getProperty("to").toString(),
                        row.getProperty("relType") != null ? row.getProperty("relType").toString() : "RELATED_TO",
                        row.getProperty("trustLevel") != null ? row.getProperty("trustLevel").toString() : null,
                        row.getProperty("note") != null ? row.getProperty("note").toString() : null));
            }
            if (!edges.isEmpty())
                return new GraphQueryResult(entityName, d, edges.size(), edges, "ArcadeDB graph_query");
        } catch (Exception e) {
            log.warn("ArcadeDB graph_query failed, falling back to PG (entity={})", entityName, e);
        }

        // PG fallback
        List<CharacterRelationship> outbound = rels.findByProjectAndFromChar(proj, entityName);
        List<CharacterRelationship> inbound = rels.findByProjectAndToChar(proj, entityName);
        List<GraphEdge> pgEdges = new ArrayList<>();
        for (var r : outbound)
            pgEdges.add(new GraphEdge(r.getFromChar(), r.getToChar(), r.getRelationType(), r.getTrustLevel(), r.getNote()));
        for (var r : inbound)
            pgEdges.add(new GraphEdge(r.getFromChar(), r.getToChar(), r.getRelationType(), r.getTrustLevel(), r.getNote()));
        return new GraphQueryResult(entityName, d, pgEdges.size(), pgEdges, "PG fallback");
    }

    @McpTool(name = "graph_path", description = "查询两节点间路径 | CN 查询两节点路径 / JP 2ノード間経路検索 / EN Find path between two nodes")
    public GraphPathResult path(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "起始角色", required = true) String from,
            @McpToolParam(description = "目标角色", required = true) String to) {

        var proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        try (RemoteDatabase db = arcadeDB.open(projectId)) {
            var result = db.query("cypher", """
                    MATCH p = shortestPath(
                        (a:Character {name: $from})-[*..10]-(b:Character {name: $to}))
                    RETURN [n IN nodes(p) | n.name] AS path, length(p) AS distance
                    """, Map.of("from", from, "to", to));
            if (result.hasNext()) {
                var row = result.next();
                @SuppressWarnings("unchecked")
                var path = (List<String>) row.getProperty("path");
                int dist = ((Number) row.getProperty("distance")).intValue();
                return new GraphPathResult(from, to, dist, path, "ArcadeDB shortestPath");
            }
            return new GraphPathResult(from, to, -1, null, "ArcadeDB: 无路径");
        } catch (Exception e) {
            log.warn("ArcadeDB graph_path failed, falling back to PG BFS ({}→{})", from, to, e);
        }

        // PG BFS fallback
        List<CharacterRelationship> allOut = rels.findByProject(proj);
        Map<String, List<String>> adj = new LinkedHashMap<>();
        for (var r : allOut) adj.computeIfAbsent(r.getFromChar(), k -> new ArrayList<>()).add(r.getToChar());
        List<String> bfsPath = bfs(adj, from, to, 5);
        return new GraphPathResult(from, to, bfsPath != null ? bfsPath.size() - 1 : -1, bfsPath, "PG BFS fallback (depth=5)");
    }

    private List<String> bfs(Map<String, List<String>> adj, String start, String target, int maxDepth) {
        record BfsNode(String name, List<String> path) {
        }
        Deque<BfsNode> q = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        q.add(new BfsNode(start, List.of(start)));
        visited.add(start);
        while (!q.isEmpty()) {
            BfsNode cur = q.poll();
            if (cur.name.equals(target)) return cur.path;
            if (cur.path.size() >= maxDepth + 1) continue;
            for (String next : adj.getOrDefault(cur.name, List.of())) {
                if (!visited.contains(next)) {
                    visited.add(next);
                    List<String> newPath = new ArrayList<>(cur.path);
                    newPath.add(next);
                    q.add(new BfsNode(next, newPath));
                }
            }
        }
        return null;
    }

    public record GraphQueryResult(String center, int depth, int edgeCount, List<GraphEdge> edges, String note) {
    }

    public record GraphEdge(String from, String to, String relationType, String trustLevel, String note) {
    }

    public record GraphPathResult(String from, String to, int distance, List<String> path, String note) {
    }
}
