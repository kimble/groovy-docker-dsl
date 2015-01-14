package com.developerb.dm.domain.docker;

import com.github.dockerjava.api.model.Link;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import java.util.Iterator;
import java.util.Set;

/**
 * For defining container dependencies
 */
public class ContainerLinks implements Iterable<ContainerLinks.ContainerLink> {

    private final Set<ContainerLink> links = Sets.newHashSet();

    @Override
    public Iterator<ContainerLink> iterator() {
        return links.iterator();
    }

    public ContainerLink add(String formatted) {
        ContainerLink link = new ContainerLink(formatted);
        links.add(link);
        return link;
    }

    public Link[] toLinks() {
        int counter = 0;
        Link[] containerLinks = new Link[links.size()];
        for (ContainerLink link : links) {
            containerLinks[counter++] = link.toLink();
        }

        return containerLinks;
    }


    public static class ContainerLink {

        private final String containerName;
        private final String alias;

        public ContainerLink(String formatted) {
            this(formatted.split(":")[0], formatted.split(":").length > 1 ? formatted.split(":")[1] : formatted);
        }

        public ContainerLink(String containerName, String alias) {
            this.containerName = containerName;
            this.alias = alias;
        }

        public Link toLink() {
            return new Link(containerName, alias);
        }

        public String containerName() {
            return containerName;
        }

        public String alias() {
            return alias;
        }

        public boolean hasDifferentAlias() {
            return !containerName.equals(alias);
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ContainerLink containerLink = (ContainerLink) o;
            return alias.equals(containerLink.alias) && containerName.equals(containerLink.containerName);
        }

        @Override
        public int hashCode() {
            int result = containerName.hashCode();
            result = 31 * result + alias.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return containerName + ":" + alias;
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContainerLinks that = (ContainerLinks) o;
        return links.equals(that.links);
    }

    @Override
    public int hashCode() {
        return links.hashCode();
    }

    @Override
    public String toString() {
        return Joiner.on(", ").join(links);
    }

}
