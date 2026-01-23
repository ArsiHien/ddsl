package uet.ndh.ddsl.parser.yaml;

import uet.ndh.ddsl.core.SourceLocation;
import uet.ndh.ddsl.core.model.BoundedContext;
import uet.ndh.ddsl.core.model.valueobject.ValueObject;
import uet.ndh.ddsl.core.model.aggregate.Aggregate;
import uet.ndh.ddsl.core.model.DomainService;
import uet.ndh.ddsl.core.model.factory.Factory;
import uet.ndh.ddsl.core.model.repository.RepositoryInterface;
import uet.ndh.ddsl.core.application.applicationservice.ApplicationService;
import uet.ndh.ddsl.parser.ParseException;

import java.util.List;
import java.util.Map;

/**
 * Converts YAML bounded context data to BoundedContext AST.
 */
public class BoundedContextConverter extends BaseYamlConverter {

    private final ValueObjectConverter valueObjectConverter;
    private final AggregateConverter aggregateConverter;
    private final DomainServiceConverter domainServiceConverter;
    private final FactoryConverter factoryConverter;
    private final RepositoryConverter repositoryConverter;
    private final ApplicationServiceConverter applicationServiceConverter;

    public BoundedContextConverter() {
        this.valueObjectConverter = new ValueObjectConverter();
        this.aggregateConverter = new AggregateConverter();
        this.domainServiceConverter = new DomainServiceConverter();
        this.factoryConverter = new FactoryConverter();
        this.repositoryConverter = new RepositoryConverter();
        this.applicationServiceConverter = new ApplicationServiceConverter();
    }

    public BoundedContext convert(Map<String, Object> data, String basePackage,
            SourceLocation location, LocationTracker locationTracker, String contextPath) throws ParseException {

        String name = getRequiredString(data, "name", locationTracker.getLocationForKey(contextPath + ".name"));
        String packageSuffix = getRequiredString(data, "package", locationTracker.getLocationForKey(contextPath + ".package"));
        String fullPackageName = basePackage + "." + packageSuffix;

        BoundedContext context = new BoundedContext(location, name, fullPackageName);

        // Convert value objects
        List<Object> valueObjectsData = getOptionalList(data, "valueObjects");
        for (int i = 0; i < valueObjectsData.size(); i++) {
            Object voData = valueObjectsData.get(i);
            if (voData instanceof Map) {
                String voPath = contextPath + ".valueObjects[" + i + "]";
                SourceLocation voLocation = locationTracker.getLocationForKey(voPath);
                ValueObject vo = valueObjectConverter.convert((Map<String, Object>) voData, voLocation);
                context.addValueObject(vo);
            }
        }

        // Convert aggregates
        List<Object> aggregatesData = getOptionalList(data, "aggregates");
        for (int i = 0; i < aggregatesData.size(); i++) {
            Object aggData = aggregatesData.get(i);
            if (aggData instanceof Map) {
                String aggPath = contextPath + ".aggregates[" + i + "]";
                SourceLocation aggLocation = locationTracker.getLocationForKey(aggPath);
                Aggregate aggregate = aggregateConverter.convert((Map<String, Object>) aggData, aggLocation);
                context.addAggregate(aggregate);
            }
        }

        // Convert domain services
        List<Object> servicesData = getOptionalList(data, "domainServices");
        for (int i = 0; i < servicesData.size(); i++) {
            Object serviceData = servicesData.get(i);
            if (serviceData instanceof Map) {
                String servicePath = contextPath + ".domainServices[" + i + "]";
                SourceLocation serviceLocation = locationTracker.getLocationForKey(servicePath);
                DomainService service = domainServiceConverter.convert((Map<String, Object>) serviceData, serviceLocation);
                context.addDomainService(service);
            }
        }

        // Convert factories
        List<Object> factoriesData = getOptionalList(data, "factories");
        for (int i = 0; i < factoriesData.size(); i++) {
            Object factoryData = factoriesData.get(i);
            if (factoryData instanceof Map) {
                String factoryPath = contextPath + ".factories[" + i + "]";
                SourceLocation factoryLocation = locationTracker.getLocationForKey(factoryPath);
                Factory factory = factoryConverter.convert((Map<String, Object>) factoryData, factoryLocation);
                context.addFactory(factory);
            }
        }

        // Convert repositories
        List<Object> repositoriesData = getOptionalList(data, "repositories");
        for (int i = 0; i < repositoriesData.size(); i++) {
            Object repoData = repositoriesData.get(i);
            if (repoData instanceof Map) {
                String repoPath = contextPath + ".repositories[" + i + "]";
                SourceLocation repoLocation = locationTracker.getLocationForKey(repoPath);
                RepositoryInterface repo = repositoryConverter.convert((Map<String, Object>) repoData, repoLocation);
                context.addRepository(repo);
            }
        }

        // Convert application services
        List<Object> appServicesData = getOptionalList(data, "applicationServices");
        for (int i = 0; i < appServicesData.size(); i++) {
            Object serviceData = appServicesData.get(i);
            if (serviceData instanceof Map) {
                String servicePath = contextPath + ".applicationServices[" + i + "]";
                SourceLocation serviceLocation = locationTracker.getLocationForKey(servicePath);
                ApplicationService service = applicationServiceConverter.convert((Map<String, Object>) serviceData, serviceLocation, locationTracker, servicePath);
                context.addApplicationService(service);
            }
        }

        return context;
    }
}
