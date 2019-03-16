package com.licenta.repository;

import com.licenta.model.Link;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<Link, Integer> {


    Optional<Link> findTopByOrderByLinkIdDesc();
}
