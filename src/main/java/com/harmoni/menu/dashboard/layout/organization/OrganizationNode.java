package com.harmoni.menu.dashboard.layout.organization;

import com.harmoni.menu.dashboard.dto.OrganizationTreeDto;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * A single row in the organization tree. Wraps the matching DTO node so the
 * detail panel can read the settings without another lookup.
 */
@Getter
public class OrganizationNode {

    /** The level of the node in the brand → chain → store hierarchy. */
    public enum Type {
        BRAND, CHAIN, STORE
    }

    private final Type type;
    private final Integer id;
    private final String name;
    private final OrganizationNode parent;
    private final List<OrganizationNode> children = new ArrayList<>();

    private final OrganizationTreeDto.BrandNode brand;
    private final OrganizationTreeDto.ChainNode chain;
    private final OrganizationTreeDto.StoreNode store;

    private OrganizationNode(Type type, Integer id, String name, OrganizationNode parent,
                             OrganizationTreeDto.BrandNode brand,
                             OrganizationTreeDto.ChainNode chain,
                             OrganizationTreeDto.StoreNode store) {
        this.type = type;
        this.id = id;
        this.name = name;
        this.parent = parent;
        this.brand = brand;
        this.chain = chain;
        this.store = store;
    }

    public static OrganizationNode of(OrganizationTreeDto.BrandNode brand) {
        return new OrganizationNode(Type.BRAND, brand.getId(), brand.getName(), null, brand, null, null);
    }

    public static OrganizationNode of(OrganizationTreeDto.ChainNode chain, OrganizationNode parent) {
        return new OrganizationNode(Type.CHAIN, chain.getId(), chain.getName(), parent, null, chain, null);
    }

    public static OrganizationNode of(OrganizationTreeDto.StoreNode store, OrganizationNode parent) {
        return new OrganizationNode(Type.STORE, store.getId(), store.getName(), parent, null, null, store);
    }

    public void addChild(OrganizationNode child) {
        children.add(child);
    }

    /** The number of chains under a brand, or stores under a chain. */
    public int childCount() {
        return children.size();
    }

    /** The total number of stores at or below this node. */
    public int storeCount() {
        if (type == Type.STORE) {
            return 1;
        }
        return children.stream().mapToInt(OrganizationNode::storeCount).sum();
    }

    public boolean isBrand() {
        return type == Type.BRAND;
    }

    public boolean isChain() {
        return type == Type.CHAIN;
    }

    public boolean isStore() {
        return type == Type.STORE;
    }
}
