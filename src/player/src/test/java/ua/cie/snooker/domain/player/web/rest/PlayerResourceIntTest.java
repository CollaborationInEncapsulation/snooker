package ua.cie.snooker.domain.player.web.rest;

import ua.cie.snooker.domain.player.PlayerApp;

import ua.cie.snooker.domain.player.domain.Player;
import ua.cie.snooker.domain.player.repository.PlayerRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the PlayerResource REST controller.
 *
 * @see PlayerResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = PlayerApp.class)
public class PlayerResourceIntTest {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    @Inject
    private PlayerRepository playerRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restPlayerMockMvc;

    private Player player;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        PlayerResource playerResource = new PlayerResource();
        ReflectionTestUtils.setField(playerResource, "playerRepository", playerRepository);
        this.restPlayerMockMvc = MockMvcBuilders.standaloneSetup(playerResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Player createEntity(EntityManager em) {
        Player player = new Player()
                .name(DEFAULT_NAME);
        return player;
    }

    @Before
    public void initTest() {
        player = createEntity(em);
    }

    @Test
    @Transactional
    public void createPlayer() throws Exception {
        int databaseSizeBeforeCreate = playerRepository.findAll().size();

        // Create the Player

        restPlayerMockMvc.perform(post("/api/players")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(player)))
            .andExpect(status().isCreated());

        // Validate the Player in the database
        List<Player> playerList = playerRepository.findAll();
        assertThat(playerList).hasSize(databaseSizeBeforeCreate + 1);
        Player testPlayer = playerList.get(playerList.size() - 1);
        assertThat(testPlayer.getName()).isEqualTo(DEFAULT_NAME);
    }

    @Test
    @Transactional
    public void createPlayerWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = playerRepository.findAll().size();

        // Create the Player with an existing ID
        Player existingPlayer = new Player();
        existingPlayer.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restPlayerMockMvc.perform(post("/api/players")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(existingPlayer)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<Player> playerList = playerRepository.findAll();
        assertThat(playerList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllPlayers() throws Exception {
        // Initialize the database
        playerRepository.saveAndFlush(player);

        // Get all the playerList
        restPlayerMockMvc.perform(get("/api/players?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(player.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())));
    }

    @Test
    @Transactional
    public void getPlayer() throws Exception {
        // Initialize the database
        playerRepository.saveAndFlush(player);

        // Get the player
        restPlayerMockMvc.perform(get("/api/players/{id}", player.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(player.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingPlayer() throws Exception {
        // Get the player
        restPlayerMockMvc.perform(get("/api/players/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updatePlayer() throws Exception {
        // Initialize the database
        playerRepository.saveAndFlush(player);
        int databaseSizeBeforeUpdate = playerRepository.findAll().size();

        // Update the player
        Player updatedPlayer = playerRepository.findOne(player.getId());
        updatedPlayer
                .name(UPDATED_NAME);

        restPlayerMockMvc.perform(put("/api/players")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedPlayer)))
            .andExpect(status().isOk());

        // Validate the Player in the database
        List<Player> playerList = playerRepository.findAll();
        assertThat(playerList).hasSize(databaseSizeBeforeUpdate);
        Player testPlayer = playerList.get(playerList.size() - 1);
        assertThat(testPlayer.getName()).isEqualTo(UPDATED_NAME);
    }

    @Test
    @Transactional
    public void updateNonExistingPlayer() throws Exception {
        int databaseSizeBeforeUpdate = playerRepository.findAll().size();

        // Create the Player

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restPlayerMockMvc.perform(put("/api/players")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(player)))
            .andExpect(status().isCreated());

        // Validate the Player in the database
        List<Player> playerList = playerRepository.findAll();
        assertThat(playerList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deletePlayer() throws Exception {
        // Initialize the database
        playerRepository.saveAndFlush(player);
        int databaseSizeBeforeDelete = playerRepository.findAll().size();

        // Get the player
        restPlayerMockMvc.perform(delete("/api/players/{id}", player.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Player> playerList = playerRepository.findAll();
        assertThat(playerList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
