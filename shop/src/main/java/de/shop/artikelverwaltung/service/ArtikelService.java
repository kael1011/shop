package de.shop.artikelverwaltung.service;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.jboss.logging.Logger;

import com.google.common.base.Strings;

import de.shop.artikelverwaltung.domain.Artikel;
import de.shop.util.interceptor.Log;

/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen
 *         Zimmermann</a>
 */
@Log
public class ArtikelService implements Serializable {
	private static final long serialVersionUID = 3076865030092242363L;
	private static final Logger LOGGER = Logger.getLogger(MethodHandles
			.lookup().lookupClass());

	@Inject
	private transient EntityManager em;

	@PostConstruct
	private void postConstruct() {
		LOGGER.debugf("CDI-faehiges Bean %s wurde erzeugt", this);
	}

	@PreDestroy
	private void preDestroy() {
		LOGGER.debugf("CDI-faehiges Bean %s wird geloescht", this);
	}

	/**
	 * Suche nach verfuegbaren Artikeln.
	 * 
	 * @return Liste der verfuegbaren Artikel.
	 */
	public List<Artikel> findVerfuegbareArtikel() {
		return em.createNamedQuery(Artikel.FIND_VERFUEGBARE_ARTIKEL,
				Artikel.class).getResultList();
	}

	/**
	 * Suche den Artikel zu gegebener ID.
	 * 
	 * @param id
	 *            ID des gesuchten Artikels.
	 * @return Der gefundene Artikel, null sonst.
	 */
	public Artikel findArtikelById(Long id) {
		return em.find(Artikel.class, id);
	}

	/**
	 * Suche die Artikel zu gegebenen IDs.
	 * 
	 * @param ids
	 *            Liste der IDs
	 * @return Liste der gefundenen Artikel
	 */
	public List<Artikel> findArtikelByIds(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}

		/*
		 * SELECT a FROM Artikel a WHERE a.id = ? OR a.id = ? OR ...
		 */
		final CriteriaBuilder builder = em.getCriteriaBuilder();
		final CriteriaQuery<Artikel> criteriaQuery = builder
				.createQuery(Artikel.class);
		final Root<Artikel> a = criteriaQuery.from(Artikel.class);

		final Path<Long> idPath = a.get("id");
		// final Path<String> idPath = a.get(Artikel_.id); // Metamodel-Klassen
		// funktionieren nicht mit Eclipse

		Predicate pred = null;
		if (ids.size() == 1) {
			// Genau 1 id: kein OR notwendig
			pred = builder.equal(idPath, ids.get(0));
		}
		else {
			// Mind. 2x id, durch OR verknuepft
			final Predicate[] equals = new Predicate[ids.size()];
			int i = 0;
			for (Long id : ids) {
				equals[i++] = builder.equal(idPath, id);
			}

			pred = builder.or(equals);
		}

		criteriaQuery.where(pred);

		return em.createQuery(criteriaQuery).getResultList();
	}

	/**
	 * Suche die Artikel mit gleicher Bezeichnung
	 * 
	 * @param bezeichnung
	 *            Gemeinsame Bezeichnung der gesuchten Artikel
	 * @return Liste der gefundenen Artikel
	 */
	public List<Artikel> findArtikelByBezeichnung(String bezeichnung) {
		if (Strings.isNullOrEmpty(bezeichnung)) {
			return findVerfuegbareArtikel();
		}

		return em
				.createNamedQuery(Artikel.FIND_ARTIKEL_BY_BEZ, Artikel.class)
				.setParameter(Artikel.PARAM_BEZEICHNUNG,
						"%" + bezeichnung + "%").getResultList();
	}

	/**
	 * Suche Artikel, die preiswerter als ein bestimmter Preis sind
	 * 
	 * @param preis
	 *            Maximaler Preis
	 * @return Liste der Artikel mit einem geringeren Preis als die angegebene
	 *         Obergrenze
	 */
	public List<Artikel> findArtikelByMaxPreis(BigDecimal preis) {
		return em
				.createNamedQuery(Artikel.FIND_ARTIKEL_MAX_PREIS, Artikel.class)
				.setParameter(Artikel.PARAM_PREIS, preis).getResultList();
	}

	// TODO Create Artikel

	public <T extends Artikel> T createArtikel(T artikel) {
		if (artikel == null) {
			return artikel;
		}

		try {
			em.createNamedQuery(Artikel.FIND_ARTIKEL_BY_BEZ, Artikel.class)
					.setParameter(Artikel.PARAM_BEZEICHNUNG,
							artikel.getBezeichnung()).getSingleResult();
			throw new BezeichnungExistsException(artikel.getBezeichnung());
		} catch (NoResultException e) {
			// Kein Artikel mit der Bezeichnung
			LOGGER.trace("Kein Artikel mit der Bezeichnung vorhanden");
		}

		em.persist(artikel);
		return artikel;
	}

	// TODO Methode schreiben

	public Artikel findArtikel(String bezeichnung) {

		return em.createNamedQuery(Artikel.FIND_ARTIKEL_BY_BEZ, Artikel.class)
				.setParameter(Artikel.PARAM_BEZEICHNUNG, bezeichnung)
				.getSingleResult();

	}

	public <T extends Artikel> T updateArtikel(T artikel) {
		if (artikel == null) {
			return null;
		}

		em.detach(artikel);

		final List<Artikel> tmp = findArtikelByBezeichnung(artikel
				.getBezeichnung());

		for (Artikel a : tmp) {
			em.detach(a);
			if (a.getId().longValue() != artikel.getId().longValue()) {
				// anderes Objekt mit gleichem Attributwert fuer Bezeichnung
				throw new BezeichnungExistsException(artikel.getBezeichnung());
			}
		}

		artikel = em.merge(artikel);
		return artikel;
	}

}
