package at.ac.tuwien.inso.ctrl.file;

import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VideoTranscodingService {
  private static final Duration TRANSCODING_TIMEOUT = Duration.ofMinutes(3);
  private static final int DEFAULT_MAX_VIDEO_SIDE = 1920;
  private static final long VIDEO_SCALE_FALLBACK_SIZE_BYTES = 25L * 1024L * 1024L;

  @Value("${ffmpeg.executable:ffmpeg}")
  private String ffmpegExecutable;

  @Value("${ffprobe.executable:ffprobe}")
  private String ffprobeExecutable;

  public void transcodeToMp4(Path inputFile, Path outputFile) {
    transcodeToMp4(inputFile, outputFile, DEFAULT_MAX_VIDEO_SIDE);
  }

  public void transcodeToMp4(Path inputFile, Path outputFile, int maxVideoSide) {
    List<String> command = new ArrayList<>();
    command.add(ffmpegExecutable);
    command.add("-y");
    command.add("-hide_banner");
    command.add("-i");
    command.add(inputFile.toString());
    command.add("-map");
    command.add("0:v:0");
    command.add("-map");
    command.add("0:a:0?");
    command.add("-c:v");
    command.add("libx264");
    command.add("-preset");
    command.add("veryfast");
    command.add("-crf");
    command.add("24");
    command.add("-vf");
    command.add(
        "scale='min("
            + maxVideoSide
            + ",iw)':'min("
            + maxVideoSide
            + ",ih)':force_original_aspect_ratio=decrease,format=yuv420p");
    command.add("-tag:v");
    command.add("avc1");
    command.add("-c:a");
    command.add("aac");
    command.add("-b:a");
    command.add("128k");
    command.add("-movflags");
    command.add("+faststart");
    command.add(outputFile.toString());

    runFfmpeg(command, outputFile, "Video could not be converted to MP4");
  }

  public boolean shouldScaleVideo(Path inputFile, int maxVideoSide) {
    return readVideoDimensions(inputFile)
        .map(dimensions -> Math.max(dimensions.width(), dimensions.height()) > maxVideoSide)
        .orElseGet(
            () -> {
              try {
                return Files.size(inputFile) > VIDEO_SCALE_FALLBACK_SIZE_BYTES;
              } catch (IOException e) {
                return true;
              }
            });
  }

  public Optional<VideoDimensions> readVideoDimensions(Path inputFile) {
    List<String> command =
        List.of(
            ffprobeExecutable,
            "-v",
            "error",
            "-select_streams",
            "v:0",
            "-show_entries",
            "stream=width,height",
            "-of",
            "csv=s=x:p=0",
            inputFile.toString());

    try {
      Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      boolean completed = process.waitFor(TRANSCODING_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      if (!completed) {
        process.destroyForcibly();
        return Optional.empty();
      }

      if (process.exitValue() != 0) {
        return Optional.empty();
      }

      String output =
          new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
      String[] parts = output.split("x");
      if (parts.length != 2) {
        return Optional.empty();
      }

      int width = Integer.parseInt(parts[0].trim());
      int height = Integer.parseInt(parts[1].trim());
      if (width <= 0 || height <= 0) {
        return Optional.empty();
      }

      return Optional.of(new VideoDimensions(width, height));
    } catch (IOException | InterruptedException | NumberFormatException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return Optional.empty();
    }
  }

  private void runFfmpeg(List<String> command, Path outputFile, String failureMessage) {
    Path ffmpegLogFile = createFfmpegLogFile();
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.redirectErrorStream(true);
    processBuilder.redirectOutput(ffmpegLogFile.toFile());

    try {
      Process process = processBuilder.start();
      boolean completed = process.waitFor(TRANSCODING_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

      if (!completed) {
        process.destroyForcibly();
        throw new InvalidRequestException("Video processing timed out");
      }

      if (process.exitValue() != 0 || !Files.exists(outputFile) || Files.size(outputFile) == 0) {
        log.warn("FFmpeg command failed: {}", readFfmpegLog(ffmpegLogFile));
        throw new InvalidRequestException(failureMessage);
      }
    } catch (IOException e) {
      throw new InvalidRequestException("Video processing is currently unavailable");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InvalidRequestException("Video processing was interrupted");
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
      throw new InvalidRequestException("Video processing is currently unavailable");
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

  public record VideoDimensions(int width, int height) {}
}
