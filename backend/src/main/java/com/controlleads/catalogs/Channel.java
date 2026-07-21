package com.controlleads.catalogs;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "channels")
public class Channel extends CatalogItem {

    protected Channel() {
    }

    public Channel(String name) {
        super(name);
    }
}
