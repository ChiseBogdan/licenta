package com.licenta.model;

import javax.persistence.*;

@Entity
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "link_id", insertable=true, updatable=true, unique=true, nullable=false)
    private Integer linkId;

    private Long imdbId;
    private Long tmdbId;

    public Link(Integer linkId, Long imdbId, Long tmdbId) {
        this.linkId = linkId;
        this.imdbId = imdbId;
        this.tmdbId = tmdbId;
    }

    public Link(){}

    public Integer getLinkId() {
        return linkId;
    }

    public void setLinkId(Integer linkId) {
        this.linkId = linkId;
    }

    public Long getImdbId() {
        return imdbId;
    }

    public void setImdbId(Long imdbId) {
        this.imdbId = imdbId;
    }

    public Long getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Long tmdbId) {
        this.tmdbId = tmdbId;
    }


}
