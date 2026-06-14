package com.novelweaver.service;

/*
 * Graph Service / 关系图 / グラフ
 *
 * CN 人物关系图和路径搜索
 * JP 人物関係グラフと経路検索
 * EN Character relationship graph and path search
 */

import com.novelweaver.model.CharacterRelationship;
import com.novelweaver.repository.CharacterRelationshipRepository;
import com.novelweaver.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GraphService {

    private static final Logger log = LoggerFactory.getLogger(GraphService.class);
    private static final int MAX_DEPTH = 10;

    private final CharacterRelationshipRepository rels;
    private final ProjectRepository projects;
    private final Neo4jClient neo4j;

    public GraphService(CharacterRelationshipRepository rels, ProjectRepository projects,
                        Neo4jClient neo4j) {
        this.rels = rels;
        this.projects = projects;
        this.neo4j = neo4j;
    }


    /*
     * 关系子图 / サブグラフ / Subgraph
     *
     * CN 查询角色关系子图（Neo4j）
     * JP キャラクター関係サブグラフを照会（Neo4j）
     * EN Query character relationship subgraph (Neo4j)
     */
    @McpTool(name = "graph_query", description = "查询角色关系子图 | CN 查询关系子图 / JP 関係サブグラフ照会 / EN Query relationship subgraph")
    public GraphQueryResult query(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "角色名", required = true) String entityName,
            @McpToolParam(description = "关系深度（默认2）", required = false) Integer depth) {

        var proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        int d = Math.max(1, Math.min(depth != null ? depth : 2, MAX_DEPTH));
        if (d < 1 || d > MAX_DEPTH) {
            throw new IllegalArgumentException("Depth must be between 1 and " + MAX_DEPTH + ", got " + d);
        }

        try {
            // depth 范围受 MAX_DEPTH 限制，结合上述校验，formatted 拼接安全
            var edges = neo4j.query("""
                            MATCH (c:Character {project_id: $pid, name: $name})-[r:RELATED_TO*1..%d]-(other:Character)
                            WHERE other.project_id = $pid
                            RETURN DISTINCT c.name AS from, other.name AS to,
                                   type(r[0]) AS relType, r[0].trustLevel AS trustLevel,
                                   r[0].note AS note
                            LIMIT 100
                            """.formatted(d))
                    .bind(projectId).to("pid")
                    .bind(entityName).to("name")
                    .fetch()
                    .all()
                    .stream()
                    .map(row -> new GraphEdge(
                            row.get("from").toString(),
                            row.get("to").toString(),
                            row.get("relType") != null ? row.get("relType").toString() : "RELATED_TO",
                            row.get("trustLevel") != null ? row.get("trustLevel").toString() : null,
                            row.get("note") != null ? row.get("note").toString() : null))
                    .toList();

            return new GraphQueryResult(entityName, d, edges.size(), edges, "Neo4j 图查询");
        } catch (Exception e) {
            log.warn("Neo4j graph_query failed, falling back to PG (entity={})", entityName, e);
        }

        // PG 降级
        List<CharacterRelationship> outbound = rels.findByProjectAndFromChar(proj, entityName);
        List<CharacterRelationship> inbound = rels.findByProjectAndToChar(proj, entityName);

        List<GraphEdge> pgEdges = new ArrayList<>();
        for (var r : outbound) {
            pgEdges.add(new GraphEdge(r.getFromChar(), r.getToChar(), r.getRelationType(),
                    r.getTrustLevel(), r.getNote()));
        }
        for (var r : inbound) {
            pgEdges.add(new GraphEdge(r.getFromChar(), r.getToChar(), r.getRelationType(),
                    r.getTrustLevel(), r.getNote()));
        }

        return new GraphQueryResult(entityName, d, pgEdges.size(), pgEdges,
                "PG character_relationships 降级。");
    }


    /*
     * 路径搜索 / 経路探索 / Path
     *
     * CN 查询两角色之间最短路径
     * JP 2キャラクター間の最短経路を検索
     * EN Find shortest path between two characters
     */
    @McpTool(name = "graph_path", description = "查询两节点间路径 | CN 查询两节点路径 / JP 2ノード間経路検索 / EN Find path between two nodes")
    public GraphPathResult path(
            @McpToolParam(description = "项目ID", required = true) String projectId,
            @McpToolParam(description = "起始角色", required = true) String from,
            @McpToolParam(description = "目标角色", required = true) String to) {

        var proj = projects.findById(UUID.fromString(projectId))
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        try {
            var rows = neo4j.query("""
                            MATCH p = shortestPath(
                                (a:Character {project_id: $pid, name: $from})-[*..10]-(b:Character {project_id: $pid, name: $to}))
                            RETURN [n IN nodes(p) | n.name] AS path, length(p) AS distance
                            """)
                    .bind(projectId).to("pid")
                    .bind(from).to("from")
                    .bind(to).to("to")
                    .fetch()
                    .all();

            if (!rows.isEmpty()) {
                var first = rows.iterator().next();
                @SuppressWarnings("unchecked")
                var path = (List<String>) first.get("path");
                int dist = ((Number) first.get("distance")).intValue();
                return new GraphPathResult(from, to, dist, path, "Neo4j shortestPath");
            }
            return new GraphPathResult(from, to, -1, null, "Neo4j: 无路径");
        } catch (Exception e) {
            log.warn("Neo4j graph_path failed, falling back to PG BFS ({}→{})", from, to, e);
        }

        // PG BFS 降级
        List<CharacterRelationship> allOut = rels.findByProject(proj);
        Map<String, List<String>> adj = new LinkedHashMap<>();
        for (var r : allOut) {
            adj.computeIfAbsent(r.getFromChar(), k -> new ArrayList<>()).add(r.getToChar());
        }

        List<String> bfsPath = bfs(adj, from, to, 5);
        return new GraphPathResult(from, to, bfsPath != null ? bfsPath.size() - 1 : -1, bfsPath,
                "PG BFS 降级（max depth=5）。");
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

    // ── result records ──

    public record GraphQueryResult(String center, int depth, int edgeCount,
                                   List<GraphEdge> edges, String note) {
    }

    public record GraphEdge(String from, String to, String relationType, String trustLevel, String note) {
    }

    public record GraphPathResult(String from, String to, int distance, List<String> path, String note) {
    }
}
