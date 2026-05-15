package at.ac.tuwien.inso.ctrl.file;

import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VideoTranscodingService {
  private static final Duration TRANSCODING_TIMEOUT = Duration.ofMinutes(3);

  @Value("${ffmpeg.executable:ffmpeg}")
  private String ffmpegExecutable;

  public void transcodeToMp4(Path inputFile, Path outputFile) {
    Path ffmpegLogFile = createFfmpegLogFile();
    List<String> command =
        List.of(
            ffmpegExecutable,
            "-y",
            "-hide_banner",
            "-i",
            inputFile.toString(),
            "-map",
            "0:v:0",
            "-map",
            "0:a:0?",
            "-c:v",
            "libx264",
            "-preset",
            "veryfast",
            "-crf",
            "23",
            "-vf",
            "scale=trunc(iw/2)*2:trunc(ih/2)*2,format=yuv420p",
            "-tag:v",
            "avc1",
            "-c:a",
            "aac",
            "-b:a",
            "128k",
            "-movflags",
            "+faststart",
            outputFile.toString());

    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.redirectErrorStream(true);
    processBuilder.redirectOutput(ffmpegLogFile.toFile());

    try {
      Process process = processBuilder.start();
      boolean completed =
          process.waitFor(
              TRANSCODING_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);

      if (!completed) {
        process.destroyForcibly();
        throw new InvalidRequestException("Video conversion timed out");
      }

      if (process.exitValue() != 0 || !Files.exists(outputFile) || Files.size(outputFile) == 0) {
        log.warn("FFmpeg conversion failed: {}", readFfmpegLog(ffmpegLogFile));
        throw new InvalidRequestException("Video could not be converted to MP4");
      }
    } catch (IOException e) {
      throw new InvalidRequestException("Video conversion is currently unavailable");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InvalidRequestException("Video conversion was interrupted");
    } finally {
      try {
        Files.deleteIfExists(ffmpegLogFile);
      } catch (IOException e) {
        log.debug("Could not delete FFmpeg log file {}", ffmpegLogFile, e);
      }
    }
  }

  private Path createFfmpegLogFile() {
    try {
      return Files.createTempFile("post-media-ffmpeg-", ".log");
    } catch (IOException e) {
      throw new InvalidRequestException("Video conversion is currently unavailable");
    }
  }

  private String readFfmpegLog(Path ffmpegLogFile) {
    try {
      String output = Files.readString(ffmpegLogFile, StandardCharsets.UTF_8).trim();
      if (output.length() > 4000) {
        return output.substring(output.length() - 4000);
      }
      return output;
    } catch (IOException e) {
      return "FFmpeg output could not be read";
    }
  }
}
