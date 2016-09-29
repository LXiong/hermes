package pl.allegro.tech.hermes.integration.env;

import org.glassfish.jersey.internal.ServiceFinder;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

class SpringlessServiceIteratorProvider extends ServiceFinder.ServiceIteratorProvider {

    private static final ServiceFinder.ServiceIteratorProvider delegate = new ServiceFinder.DefaultServiceIteratorProvider();

    @Override
    public <T> Iterator<T> createIterator(Class<T> service, String serviceName, ClassLoader loader, boolean ignoreOnClassNotFound) {
        Iterator<T> iterator = delegate.createIterator(service, serviceName, loader, ignoreOnClassNotFound);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                .filter(o -> !o.getClass().getCanonicalName().equals("org.glassfish.jersey.server.spring.SpringComponentProvider"))
                .iterator();
    }

    @Override
    public <T> Iterator<Class<T>> createClassIterator(Class<T> service, String serviceName, ClassLoader loader, boolean ignoreOnClassNotFound) {
        return delegate.createClassIterator(service, serviceName, loader, ignoreOnClassNotFound);
    }
}
