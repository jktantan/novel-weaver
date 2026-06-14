package com.novelweaver.config;

/*
 * ArcadeDB 配置 —— 独立 Server 模式（容器部署），物理租户：每个项目一个独立数据库
 *
 * CN 通过 RemoteDatabase 连接 ArcadeDB Server，按 projectId 路由到对应数据库
 * JP RemoteDatabase 経由で ArcadeDB Server に接続、projectId でデータベースを切替
 * EN Connect to ArcadeDB Server via RemoteDatabase, route by projectId
 */

import com.arcadedb.remote.RemoteDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ArcadeDBManager {

    private static final Logger log = LoggerFactory.getLogger(ArcadeDBManager.class);

    private final String host;
    private final int port;
    private final String user;
    private final String password;

    public ArcadeDBManager(
            @Value("${arcadedb.host}") String host,
            @Value("${arcadedb.port}") int port,
            @Value("${arcadedb.user}") String user,
            @Value("${arcadedb.password}") String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    /**
     * 打开指定项目的图数据库。
     * 数据库命名：project-{projectId}
     */
    public RemoteDatabase open(String projectId) {
        String dbName = "project-" + projectId;
        log.debug("Opening ArcadeDB database: {}", dbName);
        return new RemoteDatabase(host, port, dbName, user, password);
    }

    /**
     * 创建项目的图数据库（如果不存在）。
     */
    public void createIfNotExists(String projectId) {
        try (RemoteDatabase sys = new RemoteDatabase(host, port, "_system", user, password)) {
            String dbName = "project-" + projectId;
            sys.command("sql", "CREATE DATABASE " + dbName);
            log.info("Created ArcadeDB database: {}", dbName);
        } catch (Exception e) {
            // database may already exist — that's fine
            log.debug("ArcadeDB database already exists or create skipped: project-{}", projectId);
        }
    }

    /**
     * 删除项目的图数据库。
     */
    public void drop(String projectId) {
        try (RemoteDatabase sys = new RemoteDatabase(host, port, "_system", user, password)) {
            String dbName = "project-" + projectId;
            sys.command("sql", "DROP DATABASE " + dbName);
            log.info("Dropped ArcadeDB database: {}", dbName);
        } catch (Exception e) {
            log.warn("Failed to drop ArcadeDB database project-{}: {}", projectId, e.getMessage());
        }
    }

    /**
     * 健康检查——ping _system 库。
     */
    public boolean isAlive() {
        try (RemoteDatabase sys = new RemoteDatabase(host, port, "_system", user, password)) {
            sys.query("sql", "SELECT 1");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
