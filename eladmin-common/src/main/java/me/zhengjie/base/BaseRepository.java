package me.zhengjie.base;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;
import com.google.common.collect.Sets;
import me.zhengjie.db.ElSpecification;
import me.zhengjie.utils.WhereFun;
import me.zhengjie.utils.WrapperUtils;
import me.zhengjie.utils.enums.DbType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * mybatisPlus & JPA 适配
 *
 * @author liaojinlong
 * @since 2020/6/29 10:22
 */
public class BaseRepository<I extends IService<T>, J extends JpaRepository<T, ID>, T, ID extends Serializable> {
    protected I mpService;
    protected J jpaRepository;
    protected DbType dbType = DbType.JPA;

    public BaseRepository(I mpService, J jpaRepository) {
        this.mpService = mpService;
        this.jpaRepository = jpaRepository;
    }

    public T save(T t) {
        T t1;
        switch (dbType) {
            case JPA:
                t1 = jpaRepository.save(t);
                break;
            case MYBATIS:
                mpService.save(t);
                t1 = t;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + dbType);
        }
        return t1;
    }


    public List<T> saveAll(List<T> entities) {
        List<T> result;
        switch (dbType) {
            case JPA:
                result = jpaRepository.saveAll(entities);
                break;
            case MYBATIS:
                mpService.saveBatch(entities);
                result = entities;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + dbType);
        }
        return result;
    }

    public void delete(T entity) {
        switch (dbType) {
            case JPA:
                jpaRepository.delete(entity);
                break;
            case MYBATIS:
                mpService.remove(WrapperUtils.excute(entity, Wrappers.query(), WhereFun.DEFAULT));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + dbType);
        }
    }

    public void deleteById(ID id) {
        switch (dbType) {
            case JPA:
                jpaRepository.deleteById(id);
                break;
            case MYBATIS:
                mpService.removeById(id);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + dbType);
        }
    }

    public void update(T columnInfo) {
        switch (dbType) {
            case JPA:
                jpaRepository.saveAndFlush(columnInfo);
                break;
            case MYBATIS:
                mpService.saveOrUpdate(columnInfo);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + dbType);
        }
    }

    public void batUpdate(List<T> entities) {
        switch (dbType) {
            case JPA:
                jpaRepository.saveAll(entities);
                break;
            case MYBATIS:
                mpService.saveOrUpdateBatch(entities);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + dbType);
        }
    }

    public T selectById(ID id) {
        T t;
        switch (dbType) {
            case JPA:
                t = jpaRepository.getOne(id);
                break;
            case MYBATIS:
                t = mpService.getById(id);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + dbType);
        }
        return t;
    }

    public Optional<T> findById(ID id) {
        return Optional.of(selectById(id));
    }

    public List<T> selectAllById(Iterable<ID> ids) {
        List<T> t;
        switch (dbType) {
            case JPA:
                t = jpaRepository.findAllById(ids);
                break;
            case MYBATIS:
                t = mpService.listByIds(Sets.newHashSet(ids));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + dbType);
        }
        return t;
    }

    public List<T> findAll(@Nullable Specification<T> spec) {
        List<T> t = null;
        switch (dbType) {
            case JPA:
                t = ((JpaSpecificationExecutor) jpaRepository).findAll(spec);
                break;
            case MYBATIS:
                t = mpFindAll(spec);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + dbType);
        }
        return t;
    }


    public Page<T> findAll(@Nullable Specification<T> spec, Pageable pageable) {
        Page<T> t = null;
        switch (dbType) {
            case JPA:
                t = ((JpaSpecificationExecutor) jpaRepository).findAll(spec, pageable);
                break;
            case MYBATIS:
                t = mpFindAllPage(spec, pageable);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + dbType);
        }
        return t;
    }

    /**
     * Mp 适配
     *
     * @param spec
     * @param pageable
     * @return
     */
    protected Page<T> mpFindAllPage(Specification<T> spec, Pageable pageable) {
        ElSpecification<T> specifications = (ElSpecification<T>) spec;
        final QueryWrapper<T> queryWrapper = specifications.getQueryWrapper();
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();
        page.setPages(pageable.getPageNumber());
        page.setSize(pageable.getPageSize());
        page.setCurrent(pageable.getOffset());
        final Sort sort = pageable.getSort();
        final ArrayList<OrderItem> orders = new ArrayList<>();
        sort.get().forEach(order -> {
            final Sort.Direction direction = order.getDirection();
            final String property = order.getProperty();
            final OrderItem orderItem;
            switch (direction) {
                case DESC:
                    orderItem = OrderItem.desc(property);
                    break;
                case ASC:
                default:
                    orderItem = OrderItem.asc(property);
                    break;

            }
            orders.add(orderItem);
        });
        page.setOrders(orders);
        return new PageImpl<T>(mpService.page(page, queryWrapper).getRecords(),
                pageable, page.getTotal());
    }

    protected List<T> mpFindAll(Specification<T> spec) {
        ElSpecification<T> specifications = (ElSpecification<T>) spec;
        final QueryWrapper<T> queryWrapper = specifications.getQueryWrapper();
        return mpService.list(queryWrapper);
    }

    public I getMpService() {
        return mpService;
    }

    public void setMpService(I mpService) {
        this.mpService = mpService;
    }

    public J getJpaRepository() {
        return jpaRepository;
    }

    public void setJpaRepository(J jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public DbType getDbType() {
        return dbType;
    }

    public void setDbType(DbType dbType) {
        this.dbType = dbType;
    }
}