package com.controlleads.catalogs;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "campaigns")
public class Campaign extends CatalogItem {
    public Campaign() {}

    public Campaign(String name) {
        super(name);
    }
}
