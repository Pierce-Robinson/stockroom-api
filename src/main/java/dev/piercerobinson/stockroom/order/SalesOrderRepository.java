package dev.piercerobinson.stockroom.order;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

	@EntityGraph(attributePaths = { "lines", "lines.product" })
	Optional<SalesOrder> findWithLinesById(Long id);

	/**
	 * Locks the order row so two requests can't change the same order at once. Lines and
	 * products are loaded lazily afterwards, so product stock is read only once it is locked too.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from SalesOrder o where o.id = :id")
	Optional<SalesOrder> findByIdForUpdate(@Param("id") Long id);

	Page<SalesOrder> findByStatus(OrderStatus status, Pageable pageable);

}
