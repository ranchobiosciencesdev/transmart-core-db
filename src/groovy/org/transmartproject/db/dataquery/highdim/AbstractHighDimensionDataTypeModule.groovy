package org.transmartproject.db.dataquery.highdim

import grails.orm.HibernateCriteriaBuilder
import org.hibernate.SessionFactory
import org.hibernate.engine.SessionImplementor
import org.hibernate.impl.CriteriaImpl
import org.springframework.beans.factory.annotation.Autowired
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint
import org.transmartproject.core.dataquery.highdim.dataconstraints.DataConstraint
import org.transmartproject.core.dataquery.highdim.projections.Projection
import org.transmartproject.core.exceptions.UnsupportedByDataTypeException
import org.transmartproject.db.dataquery.highdim.parameterproducers.DataRetrievalParameterFactory

import javax.annotation.PostConstruct

abstract class AbstractHighDimensionDataTypeModule implements HighDimensionDataTypeModule {

    @Autowired
    SessionFactory sessionFactory

    protected List<DataRetrievalParameterFactory> assayConstraintFactories

    protected List<DataRetrievalParameterFactory> dataConstraintFactories

    protected List<DataRetrievalParameterFactory> projectionFactories

    @Autowired
    HighDimensionResourceService highDimensionResourceService

    @PostConstruct
    void init() {
        this.highDimensionResourceService.registerHighDimensionDataTypeModule(this)
    }

    @Lazy volatile Set<String> supportedAssayConstraints = {
        initializeFactories()
        assayConstraintFactories.inject(new HashSet()) {
            Set accum, DataRetrievalParameterFactory elem ->
            accum.addAll elem.supportedNames
        }
    }()

    @Lazy volatile Set<String> supportedDataConstraints = {
        initializeFactories()
        dataConstraintFactories.inject(new HashSet()) {
            Set accum, DataRetrievalParameterFactory elem ->
                accum.addAll elem.supportedNames
        }
    }()

    @Lazy volatile Set<String> supportedProjections = {
        initializeFactories()
        projectionFactories.inject(new HashSet()) {
            Set accum, DataRetrievalParameterFactory elem ->
                accum.addAll elem.supportedNames
        }
    }()

    final synchronized protected initializeFactories() {
        if (assayConstraintFactories != null) {
            return // already initialized
        }

        assayConstraintFactories = createAssayConstraintFactories()
        dataConstraintFactories  = createDataConstraintFactories()
        projectionFactories      = createProjectionFactories()
    }

    abstract protected List<DataRetrievalParameterFactory> createAssayConstraintFactories()

    abstract protected List<DataRetrievalParameterFactory> createDataConstraintFactories()

    abstract protected List<DataRetrievalParameterFactory> createProjectionFactories()

    @Override
    AssayConstraint createAssayConstraint(String name, Map<String, Object> params) {
        initializeFactories()
        for (factory in assayConstraintFactories) {
            if (factory.supports(name)) {
                return factory.createFromParameters(name, params)
            }
        }

        throw new UnsupportedByDataTypeException("The data type ${this.name} " +
                "does not support the assay constraint $name")
    }

    @Override
    DataConstraint createDataConstraint(String name, Map<String, Object> params) {
        initializeFactories()
        for (factory in dataConstraintFactories) {
            if (factory.supports(name)) {
                return factory.createFromParameters(name, params)
            }
        }

        throw new UnsupportedByDataTypeException("The data type ${this.name} " +
                "does not support the data constraint $name")
    }

    @Override
    Projection createProjection(String name, Map<String, Object> params) {
        initializeFactories()
        for (factory in projectionFactories) {
            if (factory.supports(name)) {
                return factory.createFromParameters(name, params)
            }
        }

        throw new UnsupportedByDataTypeException("The data type ${this.name} " +
                "does not support the projection $name")
    }

    final protected HibernateCriteriaBuilder createCriteriaBuilder(
            Class targetClass, String alias, SessionImplementor session) {

        HibernateCriteriaBuilder builder = new HibernateCriteriaBuilder(targetClass, sessionFactory)

        /* we have to write a private here */
        if (session) {
            //force usage of a specific session (probably stateless)
            builder.criteria = new CriteriaImpl(targetClass.canonicalName,
                                                alias,
                                                session)
            builder.criteriaMetaClass = GroovySystem.metaClassRegistry.
                    getMetaClass(builder.criteria.getClass())
        } else {
            builder.createCriteriaInstance()
        }

        /* builder.instance.is(builder.criteria) */
        builder.instance.readOnly = true
        builder.instance.cacheable = false

        builder
    }
}