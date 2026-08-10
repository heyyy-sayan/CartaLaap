package com.cartalaap.media;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cartalaap.common.BadRequestException;

@Service
public class MediaStorageService {
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif");

    private final Path imageDirectory;

    public MediaStorageService(@Value("${app.media.upload-dir}") String uploadDirectory) {
        imageDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("images");
        try {
            Files.createDirectories(imageDirectory);
        } catch (IOException exception) {
            throw new MediaStorageException("Could not initialize image storage", exception);
        }
    }

    public StoredImage store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Choose an image before uploading");
        }

        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new BadRequestException("Only JPEG, PNG, and GIF images are supported");
        }

        verifyImageContents(file);
        String filename = UUID.randomUUID() + extension;
        Path target = imageDirectory.resolve(filename).normalize();
        if (!target.getParent().equals(imageDirectory)) {
            throw new BadRequestException("Invalid image filename");
        }

        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredImage(filename, file.getSize());
        } catch (IOException exception) {
            throw new MediaStorageException("Could not save the uploaded image", exception);
        }
    }

    private void verifyImageContents(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            BufferedImage image = ImageIO.read(input);
            if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
                throw new BadRequestException("The selected file is not a valid image");
            }
        } catch (IOException exception) {
            throw new BadRequestException("The selected image could not be read");
        }
    }

    public Path imageDirectory() {
        return imageDirectory;
    }

    public record StoredImage(String filename, long size) {
    }
}
