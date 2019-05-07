package br.jus.tre_pa.jreport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.jus.tre_pa.jfilter.jpa.FilterRepository;
import br.jus.tre_pa.jreport.domain.Foo;

@Repository
public interface FooRepository extends JpaRepository<Foo, Long>, FilterRepository<Foo>{

}
