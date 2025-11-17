package com.trading.hyperliquid.service.base;

import com.trading.hyperliquid.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Generic base service providing common CRUD operations for all entities.
 * Eliminates code duplication across service classes following DRY principle.
 *
 * @param <T> the entity type
 * @param <ID> the entity ID type (typically Long)
 * @param <R> the JPA repository type
 */
public abstract class BaseService<T, ID, R extends JpaRepository<T, ID>> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final R repository;
    private final String entityName;

    /**
     * Constructor for BaseService.
     *
     * @param repository the JPA repository for this entity
     * @param entityName the human-readable entity name for error messages
     */
    protected BaseService(R repository, String entityName) {
        this.repository = repository;
        this.entityName = entityName;
    }

    /**
     * Retrieve all entities.
     *
     * @return list of all entities
     */
    @Transactional(readOnly = true)
    public List<T> findAll() {
        logger.debug("Fetching all {}", entityName);
        return repository.findAll();
    }

    /**
     * Retrieve a specific entity by ID.
     *
     * @param id the entity ID
     * @return the entity
     * @throws ResourceNotFoundException if entity with given ID not found
     */
    @Transactional(readOnly = true)
    public T findById(ID id) {
        logger.debug("Fetching {} with id: {}", entityName, id);
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("%s not found with id: %s", entityName, id)
                ));
    }

    /**
     * Save an entity (create or update).
     *
     * @param entity the entity to save
     * @return the saved entity
     */
    @Transactional
    protected T save(T entity) {
        logger.debug("Saving {}", entityName);
        return repository.save(entity);
    }

    /**
     * Delete an entity.
     *
     * @param entity the entity to delete
     */
    @Transactional
    protected void delete(T entity) {
        logger.debug("Deleting {}", entityName);
        repository.delete(entity);
    }

    /**
     * Delete an entity by ID.
     *
     * @param id the entity ID to delete
     * @throws ResourceNotFoundException if entity with given ID not found
     */
    @Transactional
    public void deleteById(ID id) {
        logger.debug("Deleting {} with id: {}", entityName, id);
        T entity = findById(id);
        delete(entity);
        logger.info("Deleted {} with id: {}", entityName, id);
    }

    /**
     * Check if an entity exists by ID.
     *
     * @param id the entity ID
     * @return true if entity exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean existsById(ID id) {
        return repository.existsById(id);
    }

    /**
     * Get the total count of entities.
     *
     * @return the count
     */
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }
}
