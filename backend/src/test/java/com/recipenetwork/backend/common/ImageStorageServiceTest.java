package com.recipenetwork.backend.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ImageStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storesAValidImageAndReturnsAPublicPath() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        byte[] jpegBytes = encodeJpeg(200, 150);
        MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", jpegBytes);

        String publicPath = service.store(file);

        assertThat(publicPath).startsWith("/uploads/").endsWith(".jpg");
        Path stored = tempDir.resolve(publicPath.substring("/uploads/".length()));
        assertThat(Files.exists(stored)).isTrue();
        BufferedImage resultImage = ImageIO.read(stored.toFile());
        assertThat(resultImage.getWidth()).isEqualTo(200);
        assertThat(resultImage.getHeight()).isEqualTo(150);
    }

    @Test
    void resizesImagesLargerThanTheMaxDimension() throws IOException {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        byte[] jpegBytes = encodeJpeg(2000, 1000);
        MockMultipartFile file = new MockMultipartFile("image", "big.jpg", "image/jpeg", jpegBytes);

        String publicPath = service.store(file);

        Path stored = tempDir.resolve(publicPath.substring("/uploads/".length()));
        BufferedImage resultImage = ImageIO.read(stored.toFile());
        assertThat(resultImage.getWidth()).isEqualTo(1600);
        assertThat(resultImage.getHeight()).isEqualTo(800);
    }

    @Test
    void rejectsAFileThatIsNotARealImage() {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "image", "fake.jpg", "image/jpeg", "definitely not an image".getBytes());

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("INVALID_IMAGE");
    }

    @Test
    void rejectsAnEmptyFile() {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("IMAGE_MISSING");
    }

    @Test
    void rejectsAFileOverTheSizeLimit() {
        ImageStorageService service = new ImageStorageService(tempDir.toString());
        byte[] tooBig = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("image", "huge.jpg", "image/jpeg", tooBig);

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("IMAGE_TOO_LARGE");
    }

    private byte[] encodeJpeg(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }
}
