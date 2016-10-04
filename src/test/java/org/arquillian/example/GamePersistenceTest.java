package org.arquillian.example;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://community.jboss.org/people/dan.j.allen">Dan Allen</a>
 */
@RunWith(Arquillian.class)
public class GamePersistenceTest {
	@Deployment
	public static Archive<?> createDeployment() {

		// Para rodar JUnit no Eclipse precisa add
		// -Djava.util.logging.manager=org.jboss.logmanager.LogManager
		WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war").addPackage(Game.class.getPackage())
				.addAsResource("test-persistence.xml", "META-INF/persistence.xml").addAsWebInfResource("jbossas-ds.xml")
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

		return war;
	}

	private static final String[] GAME_TITLES = { "Super Mario Brothers", "Mario Kart", "F-Zero" };

	@PersistenceContext
	EntityManager em;

	@Inject
	UserTransaction utx;

	@Before
	public void preparePersistenceTest() throws Exception {
		clearData();
		insertData();
		startTransaction();
	}

	private void clearData() throws Exception {
		utx.begin();
		em.joinTransaction();
		System.out.println("Dumping old records...");
		em.createQuery("delete from Game").executeUpdate();
		utx.commit();
	}

	private void insertData() throws Exception {
		utx.begin();
		em.joinTransaction();
		System.out.println("Inserting records...");
		for (String title : GAME_TITLES) {
			Game game = new Game(title);
			em.persist(game);
		}
		utx.commit();
		// reset the persistence context (cache)
		em.clear();
	}

	private void startTransaction() throws Exception {
		utx.begin();
		em.joinTransaction();
	}

	@After
	public void commitTransaction() throws Exception {
		utx.commit();
	}

	@Test
	public void shouldFindAllGamesUsingJpqlQuery() throws Exception {
		String fetchingAllGamesInJpql = "select g from Game g order by g.id";

		System.out.println("Selecting (using JPQL)...");
		List<Game> games = em.createQuery(fetchingAllGamesInJpql, Game.class).getResultList();

		System.out.println("Found " + games.size() + " games (using JPQL):");
		assertContainsAllGames(games);
	}

	@Test
	public void shouldFindAllGamesUsingCriteriaApi() throws Exception {
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Game> criteria = builder.createQuery(Game.class);

		Root<Game> game = criteria.from(Game.class);
		criteria.select(game);
		criteria.orderBy(builder.asc(game.get("id")));

		System.out.println("Selecting (using Criteria)...");
		List<Game> games = em.createQuery(criteria).getResultList();

		System.out.println("Found " + games.size() + " games (using Criteria):");
		assertContainsAllGames(games);
	}

	private static void assertContainsAllGames(Collection<Game> retrievedGames) {
		Assert.assertEquals(GAME_TITLES.length, retrievedGames.size());
		final Set<String> retrievedGameTitles = new HashSet<String>();
		for (Game game : retrievedGames) {
			System.out.println("* " + game);
			retrievedGameTitles.add(game.getTitle());
		}
		Assert.assertTrue(retrievedGameTitles.containsAll(Arrays.asList(GAME_TITLES)));
	}
}
