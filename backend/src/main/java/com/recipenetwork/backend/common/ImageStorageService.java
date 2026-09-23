package com.recipenetwork.backend.common;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores user-uploaded images on local disk. The upload is never trusted as-is: it's
 * decoded with ImageIO (rejecting anything that isn't a real, decodable image - blocks a
 * disguised non-image file with an image extension/content-type), then always re-encoded
 * to JPEG under a random filename. Re-encoding rebuilds the file from decoded pixel data,
 * which both strips embedded metadata/any polyglot payload and means the original
 * filename/extension never reaches the filesystem or the URL we hand back.
 */
@Service
public class ImageStorageService {

    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final int MAX_DIMENSION = 1600;

    private final Path uploadsDir;

    public ImageStorageService(@Value("${app.uploads-dir}") String uploadsDirProperty) {
        this.uploadsDir = Path.of(uploadsDirProperty).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadsDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Kunde inte skapa uppladdningsmappen.", e);
        }
    }

    /** Returns the public path (e.g. "/uploads/&lt;uuid&gt;.jpg") the image is now served from. */
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_MISSING", "Ingen bild valdes.");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IMAGE_TOO_LARGE", "Bilden får vara högst 5 MB.");
        }

        BufferedImage decoded;
        try (InputStream in = file.getInputStream()) {
            decoded = ImageIO.read(in);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IMAGE_READ_FAILED", "Kunde inte läsa bilden.");
        }
        if (decoded == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE", "Filen är inte en giltig bild.");
        }

        BufferedImage flattened = toOpaqueRgb(resizeIfNeeded(decoded));
        String filename = UUID.randomUUID() + ".jpg";
        Path target = uploadsDir.resolve(filename);
        try {
            ImageIO.write(flattened, "jpg", target.toFile());
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_WRITE_FAILED", "Kunde inte spara bilden.");
        }

        return "/uploads/" + filename;
    }

    private BufferedImage resizeIfNeeded(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) {
            return source;
        }
        double scale = (double) MAX_DIMENSION / Math.max(width, height);
        int newWidth = Math.max(1, (int) Math.round(width * scale));
        int newHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return scaled;
    }

    private BufferedImage toOpaqueRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        // JPEG has no alpha channel - flatten any transparency onto a white background
        // rather than letting ImageIO fail or silently corrupt the write.
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, source.getWidth(), source.getHeight());
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return rgb;
    }
}
