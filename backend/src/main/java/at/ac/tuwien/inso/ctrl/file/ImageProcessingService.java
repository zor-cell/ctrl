package at.ac.tuwien.inso.ctrl.file;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.stereotype.Service;

@Service
public class ImageProcessingService {
  static final int PROFILE_MAX_SIDE = 512;
  static final float PROFILE_JPEG_QUALITY = 0.75f;
  static final int POST_MAX_SIDE = 1920;
  static final long POST_COMPRESS_THRESHOLD_BYTES = 2L * 1024L * 1024L;
  static final float POST_JPEG_QUALITY = 0.85f;
  static final int THUMBNAIL_MAX_SIDE = 480;
  static final float THUMBNAIL_JPEG_QUALITY = 0.75f;

  private static final String JPEG_CONTENT_TYPE = "image/jpeg";
  private static final String JPEG_EXTENSION = ".jpeg";

  public ProcessedImage processProfilePicture(InputStream inputStream) throws IOException {
    BufferedImage source = readImage(inputStream);
    BufferedImage resized = resizeToMaxSide(source, PROFILE_MAX_SIDE);
    return toJpegProcessedImage(resized, PROFILE_JPEG_QUALITY);
  }

  public PostImageProcessingResult processPostImage(InputStream inputStream, long fileSize)
      throws IOException {
    BufferedImage source = readImage(inputStream);
    int maxSide = Math.max(source.getWidth(), source.getHeight());
    boolean compressMain =
        maxSide > POST_MAX_SIDE || fileSize > POST_COMPRESS_THRESHOLD_BYTES;

    byte[] thumbnailData =
        writeJpeg(resizeToMaxSide(source, THUMBNAIL_MAX_SIDE), THUMBNAIL_JPEG_QUALITY);

    ProcessedImage main = null;
    if (compressMain) {
      BufferedImage resized = resizeToMaxSide(source, POST_MAX_SIDE);
      main = toJpegProcessedImage(resized, POST_JPEG_QUALITY);
    }

    return new PostImageProcessingResult(main, thumbnailData);
  }

  public byte[] createThumbnail(InputStream inputStream) throws IOException {
    BufferedImage source = readImage(inputStream);
    BufferedImage thumbnail = resizeToMaxSide(source, THUMBNAIL_MAX_SIDE);
    return writeJpeg(thumbnail, THUMBNAIL_JPEG_QUALITY);
  }

  private BufferedImage readImage(InputStream inputStream) throws IOException {
    BufferedImage image = ImageIO.read(inputStream);
    if (image == null) {
      throw new IOException("Unsupported or unreadable image format");
    }
    return image;
  }

  private ProcessedImage toJpegProcessedImage(BufferedImage image, float quality) throws IOException {
    return new ProcessedImage(writeJpeg(image, quality), JPEG_CONTENT_TYPE, JPEG_EXTENSION);
  }

  private byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
    BufferedImage rgbImage = toRgb(image);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
    if (!writers.hasNext()) {
      throw new IOException("No JPEG image writer available");
    }

    ImageWriter writer = writers.next();
    try (ImageOutputStream imageOutputStream =
        ImageIO.createImageOutputStream(outputStream)) {
      writer.setOutput(imageOutputStream);
      ImageWriteParam writeParam = writer.getDefaultWriteParam();
      writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      writeParam.setCompressionQuality(quality);
      writer.write(null, new IIOImage(rgbImage, null, null), writeParam);
    } finally {
      writer.dispose();
    }

    return outputStream.toByteArray();
  }

  private BufferedImage toRgb(BufferedImage image) {
    if (image.getType() == BufferedImage.TYPE_INT_RGB) {
      return image;
    }

    BufferedImage rgbImage =
        new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = rgbImage.createGraphics();
    graphics.drawImage(image, 0, 0, null);
    graphics.dispose();
    return rgbImage;
  }

  private BufferedImage resizeToMaxSide(BufferedImage source, int maxSide) {
    int width = source.getWidth();
    int height = source.getHeight();
    int longestSide = Math.max(width, height);

    if (longestSide <= maxSide) {
      return source;
    }

    double scale = (double) maxSide / longestSide;
    int targetWidth = Math.max(1, (int) Math.round(width * scale));
    int targetHeight = Math.max(1, (int) Math.round(height * scale));

    BufferedImage resized =
        new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = resized.createGraphics();
    graphics.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
    graphics.dispose();
    return resized;
  }

  public record ProcessedImage(byte[] data, String contentType, String extension) {}

  public record PostImageProcessingResult(ProcessedImage main, byte[] thumbnailData) {}
}
