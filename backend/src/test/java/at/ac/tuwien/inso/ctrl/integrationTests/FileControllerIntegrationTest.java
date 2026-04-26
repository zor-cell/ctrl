package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.ac.tuwien.inso.ctrl.exception.GlobalExceptionHandler;
import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.file.FileController;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
public class FileControllerIntegrationTest {

  private MockMvc mockMvc;

  @Mock private FileStorageService fileStorageService;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new FileController(fileStorageService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void uploadPostMedia_ShouldReturnCreatedUploadResponse() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "post.png", "image/png", new byte[] {1, 2, 3});
    FileStorageService.UploadedFile uploadedFile =
        new FileStorageService.UploadedFile("posts/media/file.png", "image/png", 3L);

    when(fileStorageService.uploadPostMedia(any())).thenReturn(uploadedFile);
    when(fileStorageService.resolveFileUrl("posts/media/file.png"))
        .thenReturn("http://localhost:3900/ctrl-dev-media/posts/media/file.png");

    mockMvc
        .perform(multipart("/files/post-media").file(file))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.mediaRef").value("posts/media/file.png"))
        .andExpect(
            jsonPath("$.mediaUrl")
                .value("http://localhost:3900/ctrl-dev-media/posts/media/file.png"))
        .andExpect(jsonPath("$.contentType").value("image/png"))
        .andExpect(jsonPath("$.size").value(3));

    verify(fileStorageService).uploadPostMedia(any());
    verify(fileStorageService).resolveFileUrl("posts/media/file.png");
  }

  @Test
  void uploadPostMedia_ShouldReturnBadRequest_WhenStorageRejectsFile() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "post.txt", "text/plain", new byte[] {1, 2, 3});

    when(fileStorageService.uploadPostMedia(any()))
        .thenThrow(new InvalidRequestException("Only image and video files are supported"));

    mockMvc
        .perform(multipart("/files/post-media").file(file))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Only image and video files are supported"));
  }
}
