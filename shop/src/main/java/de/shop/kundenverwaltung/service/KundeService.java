package de.shop.kundenverwaltung.service;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.jboss.logging.Logger;

import de.shop.bestellverwaltung.domain.Bestellposition;
import de.shop.bestellverwaltung.domain.Bestellposition_;
import de.shop.bestellverwaltung.domain.Bestellung;
import de.shop.bestellverwaltung.domain.Bestellung_;
import de.shop.kundenverwaltung.domain.AbstractKunde;
import de.shop.kundenverwaltung.domain.AbstractKunde_;
import de.shop.kundenverwaltung.domain.Wartungsvertrag;
import de.shop.util.interceptor.Log;


/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
@Log
public class KundeService implements Serializable {
	private static final long serialVersionUID = -5520738420154763865L;
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass());
	
	public enum FetchType {
		NUR_KUNDE,
		MIT_BESTELLUNGEN,
		MIT_WARTUNGSVERTRAEGEN
	}
	
	public enum OrderType {
		KEINE,
		ID
	}
	
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
	 * Suche alle Kunden.
	 * @param fetch Angabe, welche Objekte aus der DB mitgeladen werden sollen, z.B. Bestellungen.
	 * @param order Sortierreihenfolge, z.B. noch ID
	 * @return Liste der Kunden
	 */
	public List<AbstractKunde> findAllKunden(FetchType fetch, OrderType order) {
		List<AbstractKunde> kunden;
		switch (fetch) {
			case NUR_KUNDE:
				kunden = OrderType.ID.equals(order)
				         ? em.createNamedQuery(AbstractKunde.FIND_KUNDEN_ORDER_BY_ID, AbstractKunde.class)
				             .getResultList()
				         : em.createNamedQuery(AbstractKunde.FIND_KUNDEN, AbstractKunde.class)
				             .getResultList();
				break;
			
			case MIT_BESTELLUNGEN:
				kunden = em.createNamedQuery(AbstractKunde.FIND_KUNDEN_FETCH_BESTELLUNGEN, AbstractKunde.class)
						   .getResultList();
				break;

			default:
				kunden = OrderType.ID.equals(order)
		                 ? em.createNamedQuery(AbstractKunde.FIND_KUNDEN_ORDER_BY_ID, AbstractKunde.class)
		                	 .getResultList()
		                 : em.createNamedQuery(AbstractKunde.FIND_KUNDEN, AbstractKunde.class)
		                     .getResultList();
				break;
		}

		return kunden;
	}
	
	/**
	 * Suche alle Kunden mit gleichem Nachnamen
	 * @param nachname Der gemeinsame Nachname
	 * @param fetch Angabe, welche Objekte aus der DB mitgeladen werden sollen, z.B. Bestellungen.
	 * @return Liste der gefundenen Kunden
	 */
	public List<AbstractKunde> findKundenByNachname(String nachname, FetchType fetch) {
		List<AbstractKunde> kunden;
		switch (fetch) {
			case NUR_KUNDE:
				kunden = em.createNamedQuery(AbstractKunde.FIND_KUNDEN_BY_NACHNAME, AbstractKunde.class)
						   .setParameter(AbstractKunde.PARAM_KUNDE_NACHNAME, nachname)
						   .getResultList();
				break;
			
			case MIT_BESTELLUNGEN:
				kunden = em.createNamedQuery(AbstractKunde.FIND_KUNDEN_BY_NACHNAME_FETCH_BESTELLUNGEN,
						                     AbstractKunde.class)
						   .setParameter(AbstractKunde.PARAM_KUNDE_NACHNAME, nachname)
						   .getResultList();
				break;

			default:
				kunden = em.createNamedQuery(AbstractKunde.FIND_KUNDEN_BY_NACHNAME, AbstractKunde.class)
						   .setParameter(AbstractKunde.PARAM_KUNDE_NACHNAME, nachname)
						   .getResultList();
				break;
		}

		return kunden;
	}

	/**
	 * Suche alle Nachnamen mit gleichem Praefix
	 * @param nachnamePrefix Der gemeinsame Praefix
	 * @return Liste der passenden Nachnamen
	 */
	public List<String> findNachnamenByPrefix(String nachnamePrefix) {
		return em.createNamedQuery(AbstractKunde.FIND_NACHNAMEN_BY_PREFIX, String.class)
				 .setParameter(AbstractKunde.PARAM_KUNDE_NACHNAME_PREFIX, nachnamePrefix + '%')
				 .getResultList();
	}

	/**
	 * Suche einen Kunden zu gegebener ID.
	 * @param id Die gegebene ID.
	 * @param fetch Angabe, welche Objekte aus der DB mitgeladen werden sollen, z.B. Bestellungen.
	 * @return Der gefundene Kunde oder null.
	 */
	public AbstractKunde findKundeById(Long id, FetchType fetch) {
		if (id == null) {
			return null;
		}
		
		AbstractKunde kunde = null;
			switch (fetch) {
				case NUR_KUNDE:
					kunde = em.find(AbstractKunde.class, id);
					break;
				
				case MIT_BESTELLUNGEN:
					try {
						kunde = em.createNamedQuery(AbstractKunde.FIND_KUNDE_BY_ID_FETCH_BESTELLUNGEN,
																	AbstractKunde.class)
								  .setParameter(AbstractKunde.PARAM_KUNDE_ID, id)
								  .getSingleResult();
					}
					catch (NoResultException e) {
						kunde = null;
					}
					break;
					
				case MIT_WARTUNGSVERTRAEGEN:
					try {
						kunde = em.createNamedQuery(AbstractKunde.FIND_KUNDE_BY_ID_FETCH_WARTUNGSVERTRAEGE,
								                    AbstractKunde.class)
								  .setParameter(AbstractKunde.PARAM_KUNDE_ID, id)
								  .getSingleResult();
					}
					catch (NoResultException e) {
						kunde = null;
					}
					break;
	
				default:
					kunde = em.find(AbstractKunde.class, id);
					break;
			}

		return kunde;
	}

	/**
	 * Suche nach IDs mit gleichem Praefix.
	 * @param idPrefix Der gemeinsame Praefix.
	 * @return Liste der passenden Praefixe.
	 */
	public List<Long> findIdsByPrefix(String idPrefix) {
		return em.createNamedQuery(AbstractKunde.FIND_IDS_BY_PREFIX, Long.class)
				 .setParameter(AbstractKunde.PARAM_KUNDE_ID_PREFIX, idPrefix + '%')
				 .getResultList();
	}
	
	/**
	 * Suche einen Kunden zu gegebener Email-Adresse.
	 * @param email Die gegebene Email-Adresse.
	 * @return Der gefundene Kunde oder null.
	 */
	public AbstractKunde findKundeByEmail(String email) {
		try {
			return em.createNamedQuery(AbstractKunde.FIND_KUNDE_BY_EMAIL, AbstractKunde.class)
					 .setParameter(AbstractKunde.PARAM_KUNDE_EMAIL, email)
					 .getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
	}

	/**
	 * Einen neuen Kunden in der DB anlegen.
	 * @param kunde Der anzulegende Kunde.
	 * @return Der neue Kunde einschliesslich generierter ID.
	 */
	public <T extends AbstractKunde> T createKunde(T kunde) {
		if (kunde == null) {
			return kunde;
		}

		// Pruefung, ob die Email-Adresse schon existiert
		try {
			em.createNamedQuery(AbstractKunde.FIND_KUNDE_BY_EMAIL, AbstractKunde.class)
			  .setParameter(AbstractKunde.PARAM_KUNDE_EMAIL, kunde.getEmail())
			  .getSingleResult();
			throw new EmailExistsException(kunde.getEmail());
		}
		catch (NoResultException e) {
			// Noch kein Kunde mit dieser Email-Adresse
			LOGGER.trace("Email-Adresse existiert noch nicht");
		}
		
		em.persist(kunde);
		return kunde;		
	}
	/**
	 * Einen vorhandenen Kunden aktualisieren
	 * @param kunde Der Kunde mit aktualisierten Attributwerten
	 * @return Der aktualisierte Kunde
	 */
	public <T extends AbstractKunde> T updateKunde(T kunde) {
		if (kunde == null) {
			return null;
		}
		
		// kunde vom EntityManager trennen, weil anschliessend z.B. nach Id und Email gesucht wird
		em.detach(kunde);
		
		// Gibt es ein anderes Objekt mit gleicher Email-Adresse?
		final AbstractKunde	tmp = findKundeByEmail(kunde.getEmail());
		if (tmp != null) {
			em.detach(tmp);
			if (tmp.getId().longValue() != kunde.getId().longValue()) {
				// anderes Objekt mit gleichem Attributwert fuer email
				throw new EmailExistsException(kunde.getEmail());
			}
		}

		em.merge(kunde);
		return kunde;
	}

	/**
	 * Einen Kunden aus der DB loeschen, falls er existiert.
	 * @param kunde Der zu loeschende Kunde.
	 */
	public void deleteKunde(AbstractKunde kunde) {
		if (kunde == null) {
			return;
		}
		
		// Bestellungen laden, damit sie anschl. ueberprueft werden koennen
		kunde = findKundeById(kunde.getId(), FetchType.MIT_BESTELLUNGEN);
		if (kunde == null) {
			return;
		}
		
		// Gibt es Bestellungen?
		if (!kunde.getBestellungen().isEmpty()) {
			throw new KundeDeleteBestellungException(kunde);
		}

		em.remove(kunde);
	}

	/**
	 * Die Kunden mit gleicher Postleitzahl suchen.
	 * @param plz Die Postleitzahl
	 * @return Liste der passenden Kunden.
	 */
	public List<AbstractKunde> findKundenByPLZ(String plz) {
		return em.createNamedQuery(AbstractKunde.FIND_KUNDEN_BY_PLZ, AbstractKunde.class)
                 .setParameter(AbstractKunde.PARAM_KUNDE_ADRESSE_PLZ, plz)
                 .getResultList();
	}

	/**
	 * Diejenigen Kunden suchen, die seit einem bestimmten Datum registriert sind. 
	 * @param seit Das zu vergleichende Datum
	 * @return Die Liste der passenden Kunden
	 */
	public List<AbstractKunde> findKundenBySeit(Date seit) {
		return em.createNamedQuery(AbstractKunde.FIND_KUNDEN_BY_DATE, AbstractKunde.class)
                 .setParameter(AbstractKunde.PARAM_KUNDE_SEIT, seit)
                 .getResultList();
	}
	
	/**
	 * Alle Privat- und Firmenkunden suchen.
	 * @return Liste der Privat- und Firmenkunden.
	 */
	public List<AbstractKunde> findPrivatkundenFirmenkunden() {
		return em.createNamedQuery(AbstractKunde.FIND_PRIVATKUNDEN_FIRMENKUNDEN, AbstractKunde.class)
                 .getResultList();
	}
	
	/**
	 * Kunden mit gleichem Nachnamen durch eine Criteria-Query suchen.
	 * @param nachname Der gemeinsame Nachname.
	 * @return Liste der passenden Kunden
	 */
	public List<AbstractKunde> findKundenByNachnameCriteria(String nachname) {
		final CriteriaBuilder builder = em.getCriteriaBuilder();
		final CriteriaQuery<AbstractKunde> criteriaQuery = builder.createQuery(AbstractKunde.class);
		final Root<AbstractKunde> k = criteriaQuery.from(AbstractKunde.class);

		final Path<String> nachnamePath = k.get(AbstractKunde_.nachname);
		//final Path<String> nachnamePath = k.get("nachname");
		
		final Predicate pred = builder.equal(nachnamePath, nachname);
		criteriaQuery.where(pred);
		
		// Ausgabe des komponierten Query-Strings. Voraussetzung: das Modul "org.hibernate" ist aktiviert
		//LOGGER.tracef("", em.createQuery(criteriaQuery).unwrap(org.hibernate.Query.class).getQueryString());
		return em.createQuery(criteriaQuery).getResultList();
	}
	
	/**
	 * Die Kunden mit einer bestimmten Mindestbestellmenge suchen.
	 * @param minMenge Die Mindestbestellmenge
	 * @return Liste der passenden Kunden
	 */
	public List<AbstractKunde> findKundenMitMinBestMenge(short minMenge) {
		final CriteriaBuilder builder = em.getCriteriaBuilder();
		final CriteriaQuery<AbstractKunde> criteriaQuery  = builder.createQuery(AbstractKunde.class);
		final Root<AbstractKunde> k = criteriaQuery.from(AbstractKunde.class);

		final Join<AbstractKunde, Bestellung> b = k.join(AbstractKunde_.bestellungen);
		final Join<Bestellung, Bestellposition> bp = b.join(Bestellung_.bestellpositionen);
		criteriaQuery.where(builder.gt(bp.<Short>get(Bestellposition_.anzahl), minMenge))
		             .distinct(true);
		
		return em.createQuery(criteriaQuery)
		         .getResultList();
	}
	
	/**
	 * Wartungsvertrage zu einem Kunden suchen
	 * @param kundeId ID des Kunden
	 * @return Liste der Wartungsvertraege des Kunden
	 */
	public List<Wartungsvertrag> findWartungsvertraege(Long kundeId) {
		return em.createNamedQuery(Wartungsvertrag.FIND_WARTUNGSVERTRAEGE_BY_KUNDE_ID, Wartungsvertrag.class)
                 .setParameter(Wartungsvertrag.PARAM_KUNDE_ID, kundeId)
                 .getResultList();
	}
	
	/**
	 * Einen neuen Wartungsvertrag in der DB anlegen.
	 * @param wartungsvertrag Der neu anzulegende Wartungsvertrag
	 * @param kunde Der zugehoerige Kunde
	 * @return Der neu angelegte Wartungsvertrag
	 */
	public Wartungsvertrag createWartungsvertrag(Wartungsvertrag wartungsvertrag, AbstractKunde kunde) {
		if (wartungsvertrag == null || kunde == null) {
			return null;
		}
		
		kunde = findKundeById(kunde.getId(), FetchType.NUR_KUNDE);
		if (kunde == null) {
			return null;
		}
		
		wartungsvertrag.setKunde(kunde);
		kunde.addWartungsvertrag(wartungsvertrag);
		
		em.persist(wartungsvertrag);
		return wartungsvertrag;
	}
}
