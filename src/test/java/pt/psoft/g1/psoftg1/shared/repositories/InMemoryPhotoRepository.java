package pt.psoft.g1.psoftg1.shared.repositories;

import java.util.ArrayList;
import java.util.List;

public class InMemoryPhotoRepository implements PhotoRepository {
  private final List<String> deletedPhotoFiles = new ArrayList<>();

  @Override
  public void deleteByPhotoFile(String photoFile) {
    deletedPhotoFiles.add(photoFile);
  }

  public List<String> deletedPhotoFiles() {
    return List.copyOf(deletedPhotoFiles);
  }
}
