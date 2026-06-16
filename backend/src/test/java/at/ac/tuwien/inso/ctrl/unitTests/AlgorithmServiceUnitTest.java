package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import at.ac.tuwien.inso.ctrl.algorithm.dto.AlgorithmConfigDto;
import at.ac.tuwien.inso.ctrl.algorithm.entity.AlgorithmConfig;
import at.ac.tuwien.inso.ctrl.algorithm.mapper.AlgorithmMapper;
import at.ac.tuwien.inso.ctrl.algorithm.repository.AlgorithmRepository;
import at.ac.tuwien.inso.ctrl.algorithm.service.AlgorithmService;
import at.ac.tuwien.inso.ctrl.topic.entity.Topic;
import at.ac.tuwien.inso.ctrl.topic.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AlgorithmServiceUnitTest {

    @Mock private AlgorithmRepository algorithmRepository;
    @Mock private AlgorithmMapper algorithmMapper;
    @Mock private TopicRepository topicRepository;

    @InjectMocks private AlgorithmService algorithmService;

    @Captor private ArgumentCaptor<AlgorithmConfig> configCaptor;

    private User testUser;
    private AlgorithmConfig defaultConfig;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());

        defaultConfig = AlgorithmConfig.builder()
                .user(testUser)
                .weightInteraction(70)
                .weightRecency(60)
                .weightExploration(70)
                .weightSourceQuality(50)
                .weightPopularity(50)
                .weightTopic(40)
                .topics(new HashSet<>())
                .build();
    }

    @Test
    void getOrCreateConfig_ReturnsExistingConfig_WhenFound() {
        when(algorithmRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(defaultConfig));

        AlgorithmConfig result = algorithmService.getOrCreateConfig(testUser);

        assertEquals(defaultConfig, result);
        verify(algorithmRepository, never()).save(any());
    }

    @Test
    void getOrCreateConfig_CreatesNewConfig_WhenNotFound() {
        when(algorithmRepository.findByUserId(testUser.getId())).thenReturn(Optional.empty());
        when(algorithmRepository.save(any(AlgorithmConfig.class))).thenAnswer(i -> i.getArguments()[0]);

        AlgorithmConfig result = algorithmService.getOrCreateConfig(testUser);

        assertNotNull(result);
        assertEquals(70, result.getWeightInteraction());
        verify(algorithmRepository).save(any(AlgorithmConfig.class));
    }

    @Test
    void updateConfig_SetsTopicPreferenceEmbedding_WhenTopicsExist() {
        AlgorithmConfigDto updateDto = new AlgorithmConfigDto(80, 50, 40, 60, 50, 50, List.of("tech", "science"));
        Topic t1 = new Topic(); t1.setTopicId("tech");
        Topic t2 = new Topic(); t2.setTopicId("science");

        when(algorithmRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(defaultConfig));
        when(topicRepository.findAllById(updateDto.topics())).thenReturn(List.of(t1, t2));
        when(topicRepository.calculateTopicPreferenceVector(List.of("tech", "science")))
                .thenReturn(List.of(0.1f, 0.5f, 0.9f));
        when(algorithmRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(algorithmMapper.toConfig(any())).thenReturn(updateDto);

        algorithmService.updateConfig(testUser, updateDto);

        verify(algorithmRepository).save(configCaptor.capture());
        AlgorithmConfig savedConfig = configCaptor.getValue();

        assertEquals(80, savedConfig.getWeightInteraction());
        assertEquals(2, savedConfig.getTopics().size());
        assertNotNull(savedConfig.getTopicPreferenceEmbedding());
    }

    @Test
    void updateConfig_SetsEmbeddingToNull_WhenNoTopicsSelected() {
        AlgorithmConfigDto updateDto = new AlgorithmConfigDto(80, 50, 40, 60, 50, 50, List.of());

        when(algorithmRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(defaultConfig));
        when(topicRepository.findAllById(updateDto.topics())).thenReturn(List.of());
        when(algorithmRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        algorithmService.updateConfig(testUser, updateDto);

        verify(algorithmRepository).save(configCaptor.capture());
        assertNull(configCaptor.getValue().getTopicPreferenceEmbedding());
        verify(topicRepository, never()).calculateTopicPreferenceVector(any());
    }
}