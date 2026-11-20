package vanilla.health

import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.Status
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

class DatabaseHealthIndicatorSpec extends Specification {

    DataSource dataSource = Mock()
    Connection conn = Mock()
    Statement stmt = Mock()
    ResultSet rs = Mock()

    DatabaseHealthIndicator indicator = new DatabaseHealthIndicator(dataSource)

    def setup() {
        dataSource.getConnection() >> conn
        conn.createStatement() >> stmt
        stmt.executeQuery("SELECT COUNT(*) FROM product") >> rs
    }

    def "reports UP with product count when table has rows"() {
        given:
        rs.next() >> true
        rs.getLong(1) >> 4L

        when:
        def health = indicator.health()

        then:
        health.status == Status.UP
        health.details.productCount == 4L
    }

    def "reports DOWN when product table is empty"() {
        given:
        rs.next() >> true
        rs.getLong(1) >> 0L

        when:
        def health = indicator.health()

        then:
        health.status == Status.DOWN
        health.details.reason == "product table is empty"
    }

    def "reports DOWN when COUNT returns no rows"() {
        given:
        rs.next() >> false

        when:
        def health = indicator.health()

        then:
        health.status == Status.DOWN
        health.details.reason == "COUNT(*) returned no rows"
    }

    def "reports DOWN when database throws"() {
        given:
        dataSource.getConnection() >> { throw new SQLException("connection refused") }

        when:
        def health = indicator.health()

        then:
        health.status == Status.DOWN
    }
}
