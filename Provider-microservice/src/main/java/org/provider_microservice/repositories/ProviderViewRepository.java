package org.provider_microservice.repositories;

import org.provider_microservice.entities.ProviderView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;

import java.util.List;
public interface ProviderViewRepository extends JpaRepository<ProviderView, Integer> {

}
