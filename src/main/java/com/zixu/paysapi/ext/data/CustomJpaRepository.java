package com.zixu.paysapi.ext.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.transform.Transformers;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.zixu.paysapi.ext.transform.AliasToBeanResultTransformer;
import com.zixu.paysapi.ext.transform.AliasToEntityMapResultTransformer;
import com.zixu.paysapi.ext.transform.SingleTransformer;

@NoRepositoryBean
@SuppressWarnings("all")
public class CustomJpaRepository<E, ID extends Serializable> extends SimpleJpaRepository<E, ID> implements CustomRepository<E, ID> {
	private final static Log log = LogFactory.getLog(CustomJpaRepository.class);
	private final static int IN_MAX_SIZE = 1000;// in集合最大数
	private final static TypeConverter converter = new SimpleTypeConverter();
	private EntityManager entityManager;
	private JpaEntityInformation<E, ID> entityInformation;

	public CustomJpaRepository(JpaEntityInformation<E, ID> entityInformation, EntityManager entityManager) {
		super(entityInformation, entityManager);
		this.entityManager = entityManager;
		this.entityInformation = entityInformation;
	}

	@Override
	public EntityManager getEntityManager() {
		return entityManager;
	}

	@Override
	public <T> void persist(T entity) {
		BeanWrapper wrapper = new BeanWrapperImpl(entity);
		wrapper.setPropertyValue(getIdAttributeName(entity.getClass()), null);
		getEntityManager().persist(entity);
	}

	protected String getIdAttributeName(Class<?> entityClass) {
		return getEntityManager().getMetamodel().entity(entityClass).getId(String.class).getName();
	}

	@Override
	public <T> List<T> get(Class<T> entityClass, String... pks) {
		if (pks == null || pks.length == 0) {
			return new ArrayList<T>();
		}
		CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
		CriteriaQuery<T> cq = cb.createQuery(entityClass);
		Root<T> root = cq.from(entityClass);
		String name = getIdAttributeName(entityClass);
		int thousand = pks.length / IN_MAX_SIZE;
		int remainder = pks.length % IN_MAX_SIZE;
		String[] segment = new String[remainder];
		System.arraycopy(pks, pks.length - remainder, segment, 0, remainder);
		Predicate x = root.get(name).in(segment);
		for (int i = 0; i < thousand; i++) {
			segment = new String[IN_MAX_SIZE];
			System.arraycopy(pks, i * IN_MAX_SIZE, segment, 0, IN_MAX_SIZE);
			Predicate y = root.get(name).in(segment);
			x = cb.or(x, y);
		}
		cq.where(x);
		TypedQuery<T> query = getEntityManager().createQuery(cq);
		List<T> resultList = query.getResultList();
		return resultList;
	}

	@Override
	public <T> T merge(T entity) {
		return getEntityManager().merge(entity);
	}

	@Override
	public <T> void remove(T entity) {
		getEntityManager().remove(entity);
	}

	@Override
	public <T> T get(Class<T> entityClass, String primaryKey) {
		return getEntityManager().find(entityClass, primaryKey);
	}

	@Override
	public <T> T get(Class<T> entityClass, String primaryKey, Map<String, Object> properties) {
		return getEntityManager().find(entityClass, primaryKey, properties);
	}

	@Override
	public <T> T get(Class<T> entityClass, String primaryKey, LockModeType lockMode) {
		return getEntityManager().find(entityClass, primaryKey, lockMode);
	}

	@Override
	public <T> T get(Class<T> entityClass, String primaryKey, LockModeType lockMode, Map<String, Object> properties) {
		return getEntityManager().find(entityClass, primaryKey, lockMode, properties);
	}

	@Override
	public <T> T getReference(Class<T> entityClass, String primaryKey) {
		return getEntityManager().getReference(entityClass, primaryKey);
	}

	/**
	 * 设置查询参数
	 * 
	 * @param query
	 *            查询对象
	 * @param param
	 *            参数值
	 */
	protected void setQueryParams(Query query, Object param) {
		if (param != null) {
			if (param instanceof Object[]) {
				Object[] args = (Object[]) param;
				for (int i = 0; i < args.length; i++) {
					query.setParameter(i + 1, args[i]);
				}
			} else if (param instanceof Map) {
				Map<String, Object> args = (Map<String, Object>) param;
				Iterator<Entry<String, Object>> it = args.entrySet().iterator();
				while (it.hasNext()) {
					Entry<String, Object> entry = it.next();
					query.setParameter(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	/**
	 * 创建Query
	 * 
	 * @param resultClass
	 *            返回对象类型,null时返回Object
	 * @param qlString
	 *            查询语句
	 * @param isNative
	 *            是否SQL查询
	 * @return
	 */
	protected <T> Query createQuery(Class<T> resultClass, String qlString, boolean isNative) {
		Query query = null;
		if (isNative) {
			if (resultClass == null || Object[].class.isAssignableFrom(resultClass)) {
				query = getEntityManager().createNativeQuery(qlString);
			} else if (Map.class.isAssignableFrom(resultClass) || List.class.isAssignableFrom(resultClass)) {
				query = getEntityManager().createNativeQuery(qlString);
				if (query instanceof org.hibernate.jpa.HibernateQuery) {
					if (Map.class.isAssignableFrom(resultClass)) {
						((org.hibernate.jpa.HibernateQuery) query).getHibernateQuery().setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
					} else {
						((org.hibernate.jpa.HibernateQuery) query).getHibernateQuery().setResultTransformer(Transformers.TO_LIST);
					}
				}
				// } else if (resultClass.getAnnotation(Entity.class) != null) {
				// query = getEntityManager().createNativeQuery(qlString,
				// resultClass);
			} else if (String.class.isAssignableFrom(resultClass)) {
				query = getEntityManager().createNativeQuery(qlString);
				if (query instanceof org.hibernate.jpa.HibernateQuery) {
					((org.hibernate.jpa.HibernateQuery) query).getHibernateQuery().setResultTransformer(SingleTransformer.INSTANCE);
				}
			} else {
				query = getEntityManager().createNativeQuery(qlString);
				if (query instanceof org.hibernate.jpa.HibernateQuery) {
					((org.hibernate.jpa.HibernateQuery) query).getHibernateQuery().setResultTransformer(new AliasToBeanResultTransformer(resultClass));
				}
			}
		} else {
			if (resultClass == null) {
				query = getEntityManager().createQuery(qlString);
			} else {
				query = getEntityManager().createQuery(qlString, resultClass);
			}
		}
		if (query instanceof org.hibernate.jpa.HibernateQuery) {
			// ((org.hibernate.ejb.HibernateQuery)
			// query).getHibernateQuery().setCacheable(true);
		}
		return query;
	}

	/**
	 * 获取记录总数
	 * 
	 * @param qlString
	 * @param param
	 * @param isNative
	 * @return
	 */
	protected int getTotalRecords(String qlString, Object param, boolean isNative) {
		String countSql = null;
		if (isNative) {
			countSql = "select count(*) as col_0_0_ from ( " + qlString + " ) table0_";
		} else {
			int index = qlString.toLowerCase().indexOf("from ");
			countSql = qlString.substring(index);
			countSql = "select count(*) as col_0_0_ " + countSql;
		}
		Query query = createQuery(null, countSql, isNative);
		setQueryParams(query, param);
		List<Object> list = query.getResultList();
		if (list.isEmpty()) {
			return 0;
		}
		return converter.convertIfNecessary(list.get(0), Integer.class);
	}

	/**
	 * 执行查询语句返回列表
	 * 
	 * @param resultClass
	 *            返回对象类型,null时返回Object
	 * @param sqlString
	 *            查询语句
	 * @param args
	 *            参数值,支持Object[]和Map
	 * @param isNative
	 *            是否SQL查询
	 * @return
	 */
	protected <T> List<T> _find(Class<T> resultClass, String sqlString, Object args, boolean isNative) {
		Query query = createQuery(resultClass, sqlString, isNative);
		setQueryParams(query, args);
		return query.getResultList();
	}

	protected <T> T _findUnique(Class<T> resultClass, String qlString, Object args, boolean isNative) {
		List<T> list = _find(resultClass, qlString, args, isNative);
		if (list.size() == 0) {
			return null;
		}
		if (list.size() > 1) {
			throw new NonUniqueResultException("result returns more than one elements");
		}
		return list.get(0);
	}

	protected int _executeUpdate(String sqlString, Object args, boolean isNative) {
		Query query = createQuery(null, sqlString, isNative);
		setQueryParams(query, args);
		return query.executeUpdate();
	}

	@Override
	public <T> T findUnique(Class<T> resultClass, String jpql) {
		return _findUnique(resultClass, jpql, null, false);
	}

	@Override
	public <T> T findUnique(Class<T> resultClass, String jpql, Map<String, Object> args) {
		return _findUnique(resultClass, jpql, args, false);
	}

	@Override
	public <T> T findUnique(Class<T> resultClass, String jpql, Object... args) {
		return _findUnique(resultClass, jpql, args, false);
	}

	@Override
	public <T> T nativeFindUnique(Class<T> resultClass, String sql) {
		return _findUnique(resultClass, sql, null, true);
	}

	@Override
	public <T> T nativeFindUnique(Class<T> resultClass, String sql, Map<String, Object> args) {
		return _findUnique(resultClass, sql, args, true);
	}

	@Override
	public <T> T nativeFindUnique(Class<T> resultClass, String sql, Object... args) {
		return _findUnique(resultClass, sql, args, true);
	}

	@Override
	public <T> List<T> find(Class<T> resultClass, String jpql) {
		return _find(resultClass, jpql, null, false);
	}

	@Override
	public <T> List<T> find(Class<T> resultClass, String jpql, Object... args) {
		return _find(resultClass, jpql, args, false);
	}

	@Override
	public <T> List<T> find(Class<T> resultClass, String jpql, Map<String, Object> args) {
		return _find(resultClass, jpql, args, false);
	}

	@Override
	public <T> List<T> nativeFind(Class<T> resultClass, String sql) {
		return _find(resultClass, sql, null, true);
	}

	@Override
	public <T> List<T> nativeFind(Class<T> resultClass, String sql, Object... args) {
		return _find(resultClass, sql, args, true);
	}

	@Override
	public <T> List<T> nativeFind(Class<T> resultClass, String sql, Map<String, Object> args) {
		return _find(resultClass, sql, args, true);
	}

	@Override
	public int executeUpdate(String jpql) {
		return _executeUpdate(jpql, null, false);
	}

	@Override
	public int executeUpdate(String jpql, Object... args) {
		return _executeUpdate(jpql, args, false);
	}

	@Override
	public int executeUpdate(String jpql, Map<String, Object> args) {
		return _executeUpdate(jpql, args, false);
	}

	@Override
	public int nativeExecuteUpdate(String sql) {
		return _executeUpdate(sql, null, true);
	}

	@Override
	public int nativeExecuteUpdate(String sql, Object... args) {
		return _executeUpdate(sql, args, true);
	}

	@Override
	public int nativeExecuteUpdate(String sql, Map<String, Object> args) {
		return _executeUpdate(sql, args, true);
	}

	@Override
	public int getCount(String sql,Object... args) {
		// TODO Auto-generated method stub
		return getTotalRecords(sql, args, true);
	}
}