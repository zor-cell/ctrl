package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.file.VideoTranscodingService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class VideoTranscodingServiceUnitTest {

  @Test
  void transcodeToMp4_withSuccessfulProcess_createsOutputFile() throws Exception {
    Assumptions.assumeFalse(System.getProperty("os.name").toLowerCase().contains("win"));

    VideoTranscodingService service = new VideoTranscodingService();
    Path fakeFfmpeg = Files.createTempFile("fake-ffmpeg-", ".sh");
    Path inputFile = Files.createTempFile("video-input-", ".mov");
    Path outputFile = Files.createTempFile("video-output-", ".mp4");
    Files.deleteIfExists(outputFile);

    try {
      Files.writeString(
          fakeFfmpeg,
          """
          #!/bin/sh
          for arg in "$@"; do
            output="$arg"
          done
          printf converted > "$output"
          """);
      assertTrue(fakeFfmpeg.toFile().setExecutable(true));
      ReflectionTestUtils.setField(service, "ffmpegExecutable", fakeFfmpeg.toString());
      Files.write(inputFile, new byte[] {1, 2, 3});

      service.transcodeToMp4(inputFile, outputFile);

      assertTrue(Files.exists(outputFile));
      assertEquals("converted", Files.readString(outputFile));
    } finally {
      Files.deleteIfExists(fakeFfmpeg);
      Files.deleteIfExists(inputFile);
      Files.deleteIfExists(outputFile);
    }
  }

  @Test
  void transcodeToMp4_withUnavailableFfmpeg_throwsInvalidRequestException() throws Exception {
    VideoTranscodingService service = new VideoTranscodingService();
    ReflectionTestUtils.setField(service, "ffmpegExecutable", "/missing/ffmpeg");
    Path inputFile = Files.createTempFile("video-input-", ".mov");
    Path outputFile = Files.createTempFile("video-output-", ".mp4");

    try {
      Files.write(inputFile, new byte[] {1, 2, 3});

      InvalidRequestException exception =
          assertThrows(
              InvalidRequestException.class, () -> service.transcodeToMp4(inputFile, outputFile));

      assertEquals("Video conversion is currently unavailable", exception.getMessage());
    } finally {
      Files.deleteIfExists(inputFile);
      Files.deleteIfExists(outputFile);
    }
  }
}
