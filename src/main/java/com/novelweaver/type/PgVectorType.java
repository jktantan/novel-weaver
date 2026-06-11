package com.novelweaver.type;

/*
 * PGvector Hibernate Type / pgvector 类型映射 / pgvector 型マッピング
 *
 * CN String ←→ vector(1024) 的自定义 Hibernate UserType
 * JP String ←→ vector(1024) のカスタム Hibernate UserType
 * EN Custom Hibernate UserType: String ←→ vector(1024)
 */

import com.pgvector.PGvector;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.postgresql.util.PGobject;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Objects;

/**
 * Hibernate UserType: String ←→ PostgreSQL vector(1024).
 * <p>
 * Stores embedding strings like "[0.1,0.2,0.3,…]" in a pgvector column.
 * The pgvector JDBC driver (PGvector extends PGobject) handles the binary format.
 */
public class PgVectorType implements UserType<String> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<String> returnedClass() {
        return String.class;
    }

    @Override
    public boolean equals(String x, String y) {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(String x) {
        return Objects.hashCode(x);
    }

    @Override
    public String nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        Object raw = rs.getObject(position);
        if (raw == null) return null;

        // Case 1: JDBC driver returned proper PGvector
        if (raw instanceof PGvector) {
            return Arrays.toString(((PGvector) raw).toArray());
        }

        // Case 2: raw PGobject (type mapping not registered on connection)
        if (raw instanceof PGobject) {
            String val = ((PGobject) raw).getValue();
            return val; // pgvector text format is already "[0.1,0.2,…]"
        }

        // Case 3: unexpected type — use toString
        return raw.toString();
    }

    @Override
    public void nullSafeSet(PreparedStatement ps, String value, int index, SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.OTHER);
        } else {
            ps.setObject(index, new PGvector(value), Types.OTHER);
        }
    }

    @Override
    public String deepCopy(String value) {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(String value) {
        return value;
    }

    @Override
    public String assemble(Serializable cached, Object owner) {
        return (String) cached;
    }

    @Override
    public String replace(String original, String target, Object owner) {
        return original;
    }
}
