package dev.piercerobinson.stockroom.product;

import java.util.Collection;
import java.util.List;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

	boolean existsBySkuIgnoreCase(String sku);

	Page<Product> findByActiveTrue(Pageable pageable);

	@Query("""
			select p from Product p
			where p.active = true
			  and (lower(p.name) like :pattern or lower(p.sku) like :pattern)
			""")
	Page<Product> searchActive(@Param("pattern") String pattern, Pageable pageable);

	@Query("select p from Product p where p.active = true and p.quantityOnHand <= p.reorderLevel order by p.quantityOnHand")
	List<Product> findLowStock();

	/**
	 * Locks rows for update, in id order, so concurrent order confirmations cannot oversell
	 * stock or deadlock each other.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p where p.id in :ids order by p.id")
	List<Product> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

}
