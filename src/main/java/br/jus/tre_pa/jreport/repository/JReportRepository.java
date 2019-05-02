package br.jus.tre_pa.jreport.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import br.jus.tre_pa.datafilter.jpa.DataFilterRepository;
import br.jus.tre_pa.jreport.domain.JReport;


@Repository
public interface JReportRepository extends JpaRepository<JReport, Long>, JpaSpecificationExecutor<JReport> , DataFilterRepository<JReport> {
	
	@Query("select distinct r.category from JReport r")
	Page<String> findAllCategories(Pageable pageable);
}
