package org.springframework.security.acls.jdbc;

import static java.util.Objects.requireNonNull;

import com.github.benmanes.caffeine.cache.Caffeine;
import javax.sql.DataSource;
import org.molgenis.data.config.DataSourceConfig;
import org.molgenis.data.transaction.TransactionManager;
import org.molgenis.security.NoOpAuditLogger;
import org.molgenis.security.acl.AclCacheTransactionListener;
import org.molgenis.security.acl.BitMaskPermissionGrantingStrategy;
import org.molgenis.security.acl.MutableAclClassService;
import org.molgenis.security.acl.MutableAclClassServiceImpl;
import org.molgenis.security.acl.ObjectIdentityService;
import org.molgenis.security.acl.ObjectIdentityServiceImpl;
import org.molgenis.security.acl.TransactionalJdbcMutableAclService;
import org.molgenis.security.core.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl;
import org.springframework.security.acls.domain.AuditLogger;
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * TODO move to org.molgenis sub-package once we upgraded to a spring-security-acl release with
 * https://github.com/spring-projects/spring-security/issues/4814.
 */
@Configuration
@Import({DataSourceConfig.class})
public class AclConfig {
  private final DataSource dataSource;
  private final TransactionManager transactionManager;
  private final RoleHierarchy roleHierarchy;
  @Autowired JdbcTemplate jdbcTemplate;

  public AclConfig(
      DataSource dataSource, TransactionManager transactionManager, RoleHierarchy roleHierarchy) {
    this.dataSource = requireNonNull(dataSource);
    this.transactionManager = requireNonNull(transactionManager);
    this.roleHierarchy = requireNonNull(roleHierarchy);
  }

  @Bean
  public SidRetrievalStrategy sidRetrievalStrategy() {
    return new SidRetrievalStrategyImpl(roleHierarchy);
  }

  @Bean
  public AuditLogger auditLogger() {
    return new NoOpAuditLogger();
  }

  @Bean
  public PermissionGrantingStrategy permissionGrantingStrategy() {
    return new BitMaskPermissionGrantingStrategy(auditLogger());
  }

  @Bean
  public AclAuthorizationStrategy aclAuthorizationStrategy() {
    GrantedAuthority gaTakeOwnership =
        new SimpleGrantedAuthority(SecurityUtils.ROLE_ACL_TAKE_OWNERSHIP);
    GrantedAuthority gaModifyAuditing =
        new SimpleGrantedAuthority(SecurityUtils.ROLE_ACL_MODIFY_AUDITING);
    GrantedAuthority gaGeneralChanges =
        new SimpleGrantedAuthority(SecurityUtils.ROLE_ACL_GENERAL_CHANGES);
    AclAuthorizationStrategyImpl aclAuthorizationStrategy =
        new AclAuthorizationStrategyImpl(gaTakeOwnership, gaModifyAuditing, gaGeneralChanges);
    aclAuthorizationStrategy.setSidRetrievalStrategy(sidRetrievalStrategy());
    return aclAuthorizationStrategy;
  }

  @Bean
  public AclCache aclCache() {
    Cache cache = new CaffeineCache("aclCache", Caffeine.newBuilder().maximumSize(10000).build());
    return new SpringCacheBasedAclCache(
        cache, permissionGrantingStrategy(), aclAuthorizationStrategy());
  }

  @Bean
  public AclCacheTransactionListener aclCacheTransactionListener() {
    AclCacheTransactionListener aclCacheTransactionListener =
        new AclCacheTransactionListener(aclCache(), mutableAclClassService());
    transactionManager.addTransactionListener(aclCacheTransactionListener);
    return aclCacheTransactionListener;
  }

  @Bean
  public MutableAclClassService mutableAclClassService() {
    return new MutableAclClassServiceImpl(jdbcTemplate, aclCache());
  }

  @Bean
  public ObjectIdentityService objectIdentityService() {
    return new ObjectIdentityServiceImpl(jdbcTemplate);
  }

  @Bean
  public AclClassIdUtils aclClassIdUtils() {
    AclClassIdUtils aclClassIdUtils = new AclClassIdUtils();
    aclClassIdUtils.setConversionService(new DefaultConversionService());
    return aclClassIdUtils;
  }

  @Bean
  public LookupStrategy lookupStrategy() {
    BasicLookupStrategy basicLookupStrategy =
        new BasicLookupStrategy(
            dataSource, aclCache(), aclAuthorizationStrategy(), permissionGrantingStrategy());
    basicLookupStrategy.setAclClassIdSupported(true);
    basicLookupStrategy.setAclClassIdUtils(aclClassIdUtils());
    return basicLookupStrategy;
  }

  @Bean
  public MutableAclService aclService() {
    JdbcMutableAclService aclService =
        new TransactionalJdbcMutableAclService(dataSource, lookupStrategy(), aclCache());
    aclService.setAclClassIdSupported(true);
    aclService.setAclClassIdUtils(aclClassIdUtils());
    aclService.setClassIdentityQuery("select currval(pg_get_serial_sequence('acl_class', 'id'))");
    aclService.setSidIdentityQuery("select currval(pg_get_serial_sequence('acl_sid', 'id'))");
    aclService.setObjectIdentityPrimaryKeyQuery(
        "select acl_object_identity.id from acl_object_identity, acl_class where acl_object_identity.object_id_class = acl_class.id and acl_class.class=? and acl_object_identity.object_id_identity = ?::varchar");
    aclService.setFindChildrenQuery(
        "select obj.object_id_identity as obj_id, class.class as class, class.class_id_type as class_id_type "
            + "from acl_object_identity obj, acl_object_identity parent, acl_class class "
            + "where obj.parent_object = parent.id and obj.object_id_class = class.id "
            + "and parent.object_id_identity = ?::varchar and parent.object_id_class = ("
            + "select id FROM acl_class where acl_class.class = ?)");
    return aclService;
  }

  @Bean
  public AclPermissionEvaluator aclPermissionEvaluator() {
    AclPermissionEvaluator permissionEvaluator = new AclPermissionEvaluator(aclService());
    permissionEvaluator.setSidRetrievalStrategy(sidRetrievalStrategy());
    return permissionEvaluator;
  }
}
