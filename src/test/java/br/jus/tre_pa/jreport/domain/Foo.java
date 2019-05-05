package br.jus.tre_pa.jreport.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Foo {
	
	@Id
	private Long id;
	
	@Column
	private String name;
	
}
