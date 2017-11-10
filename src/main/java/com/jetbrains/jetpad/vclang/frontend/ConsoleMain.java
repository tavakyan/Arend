package com.jetbrains.jetpad.vclang.frontend;

import com.jetbrains.jetpad.vclang.frontend.storage.FileStorage;
import com.jetbrains.jetpad.vclang.frontend.storage.LibStorage;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.module.ModuleResolver;
import com.jetbrains.jetpad.vclang.module.caching.ModuleUriProvider;
import com.jetbrains.jetpad.vclang.module.source.CompositeSourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.CompositeStorage;
import com.jetbrains.jetpad.vclang.module.source.NullStorage;
import com.jetbrains.jetpad.vclang.term.Group;
import org.apache.commons.cli.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConsoleMain extends BaseCliFrontend<CompositeStorage<FileStorage.SourceId, CompositeStorage<LibStorage.SourceId, PreludeStorage.SourceId>.SourceId>.SourceId> {
  private static final Options cmdOptions = new Options();
  static {
    cmdOptions.addOption("h", "help", false, "print this message");
    cmdOptions.addOption(Option.builder("L").longOpt("libs").hasArg().argName("libdir").desc("directory containing libraries").build());
    cmdOptions.addOption(Option.builder("s").longOpt("source").hasArg().argName("srcdir").desc("project source directory").build());
    cmdOptions.addOption(Option.builder("c").longOpt("cache").hasArg().argName("cachedir").desc("directory for project-specific cache files (relative to srcdir)").build());
    cmdOptions.addOption(Option.builder().longOpt("recompile").desc("recompile files").build());
  }

  private final StorageManager storageManager;

  public ConsoleMain(Path libDir, Path sourceDir, Path cacheDir, boolean recompile) throws IOException {
    super(recompile);
    this.storageManager = new StorageManager(libDir, sourceDir, cacheDir, moduleTracker);
    initialize(storageManager.storage);
  }

  @Override
  protected String displaySource(CompositeSourceSupplier<FileStorage.SourceId, CompositeStorage<LibStorage.SourceId, PreludeStorage.SourceId>.SourceId>.SourceId source, boolean modulePathOnly) {
    StringBuilder builder = new StringBuilder();
    builder.append(source.getModulePath());
    if (!modulePathOnly) {
      if (source.source1 != null) {
        builder.append(" (").append(source.source1).append(")");
      } else if (source.source2 != null && source.source2.source1 != null) {
        builder.append(" (").append(source.source2.source1).append(")");
      }
    }
    return builder.toString();
  }

  @Override
  protected ModuleUriProvider<CompositeStorage<FileStorage.SourceId, CompositeStorage<LibStorage.SourceId, PreludeStorage.SourceId>.SourceId>.SourceId> createModuleUriProvider() {
    return new MyModuleUriProvider();
  }

  @Override
  protected Group loadPrelude() {
    return super.loadPrelude();
  }

  private class StorageManager {
    final FileStorage projectStorage;
    final LibStorage libStorage;
    final PreludeStorage preludeStorage;

    private final CompositeStorage<LibStorage.SourceId, PreludeStorage.SourceId> nonProjectCompositeStorage;
    public final CompositeStorage<FileStorage.SourceId, CompositeStorage<LibStorage.SourceId, PreludeStorage.SourceId>.SourceId> storage;

    StorageManager(Path libDir, Path projectDir, Path cacheDir, ModuleResolver moduleResolver) throws IOException {
      projectStorage = new FileStorage(projectDir, cacheDir, sourceModuleScopeProvider, moduleResolver, moduleScopeProvider);
      libStorage = libDir != null ? new LibStorage(libDir, sourceModuleScopeProvider, moduleResolver, moduleScopeProvider) : null;
      preludeStorage = new PreludeStorage(sourceModuleScopeProvider);

      nonProjectCompositeStorage = new CompositeStorage<>(libStorage != null ? libStorage : new NullStorage<>(), preludeStorage);
      storage = new CompositeStorage<>(projectStorage, nonProjectCompositeStorage);
    }

    CompositeSourceSupplier<FileStorage.SourceId, CompositeSourceSupplier<com.jetbrains.jetpad.vclang.frontend.storage.LibStorage.SourceId, com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage.SourceId>.SourceId>.SourceId idForProjectSource(FileStorage.SourceId sourceId) {
      return storage.idFromFirst(sourceId);
    }

    CompositeSourceSupplier<FileStorage.SourceId, CompositeSourceSupplier<com.jetbrains.jetpad.vclang.frontend.storage.LibStorage.SourceId, com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage.SourceId>.SourceId>.SourceId idForLibSource(LibStorage.SourceId sourceId) {
      if (libStorage == null) return null;
      return storage.idFromSecond(nonProjectCompositeStorage.idFromFirst(sourceId));
    }

    CompositeSourceSupplier<FileStorage.SourceId, CompositeSourceSupplier<com.jetbrains.jetpad.vclang.frontend.storage.LibStorage.SourceId, com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage.SourceId>.SourceId>.SourceId idForPreludeSource(PreludeStorage.SourceId sourceId) {
      return storage.idFromSecond(nonProjectCompositeStorage.idFromSecond(sourceId));
    }
  }

  private class MyModuleUriProvider implements ModuleUriProvider<CompositeStorage<FileStorage.SourceId, CompositeStorage<LibStorage.SourceId, PreludeStorage.SourceId>.SourceId>.SourceId> {
    @Override
    public @Nonnull URI getUri(CompositeStorage<FileStorage.SourceId, CompositeStorage<LibStorage.SourceId, PreludeStorage.SourceId>.SourceId>.SourceId sourceId) {
      try {
        final String scheme;
        final String root;
        final Path relPath;
        final String query;

        if (sourceId.source1 != null) {  // Project source
          scheme = "file";
          root = "";
          relPath = sourceId.source1.getRelativeFilePath();
          query = "";
        } else {
          if (sourceId.source2 == null) throw new IllegalStateException();
          if (sourceId.source2.source1 != null) {  // Lib source
            scheme = "lib";
            root = sourceId.source2.source1.getLibraryName();
            relPath = sourceId.source2.source1.fileSourceId.getRelativeFilePath();
            query = "";
          } else {  // Prelude source
            if (sourceId.source2.source2 == null) throw new IllegalStateException();
            scheme = "prelude";
            root = "";
            relPath = Paths.get("");
            query = null;
          }
        }


        return new URI(scheme, root, Paths.get("/").resolve(relPath).toUri().getPath(), query, null);
      } catch (URISyntaxException e) {
        throw new IllegalStateException();
      }
    }

    @Override
    public @Nullable CompositeStorage<FileStorage.SourceId, CompositeStorage<LibStorage.SourceId, PreludeStorage.SourceId>.SourceId>.SourceId getModuleId(URI sourceUri) {
      if ("file".equals(sourceUri.getScheme())) {
        if (sourceUri.getAuthority() != null) return null;
        try {
          Path path = Paths.get(new URI("file", null, sourceUri.getPath(), null));
          ModulePath modulePath = FileStorage.modulePath(path.getRoot().relativize(path));
          if (modulePath == null) return null;

          FileStorage.SourceId fileSourceId = storageManager.projectStorage.locateModule(modulePath);
          return fileSourceId != null ? storageManager.idForProjectSource(fileSourceId) : null;
        } catch (URISyntaxException | NumberFormatException e) {
          return null;
        }
      } else if ("lib".equals(sourceUri.getScheme())) {
        if (storageManager.libStorage == null) return null;
        try {
          String libName = sourceUri.getAuthority();
          if (libName == null) return null;
          Path path = Paths.get(new URI("file", null, sourceUri.getPath(), null));
          ModulePath modulePath = FileStorage.modulePath(path.getRoot().relativize(path));
          if (modulePath == null) return null;

          LibStorage.SourceId libSourceId = storageManager.libStorage.locateModule(libName, modulePath);
          return libSourceId != null ? storageManager.idForLibSource(libSourceId) : null;
        } catch (URISyntaxException | NumberFormatException e) {
          return null;
        }
      } else if ("prelude".equals(sourceUri.getScheme())) {
        if (sourceUri.getAuthority() != null || !sourceUri.getPath().equals("/")) return null;
        return storageManager.idForPreludeSource(storageManager.preludeStorage.preludeSourceId);
      } else {
        return null;
      }
    }

  }


  public static void main(String[] args) throws IOException {
    try {
      CommandLine cmdLine = new DefaultParser().parse(cmdOptions, args);

      if (cmdLine.hasOption("h")) {
        printHelp();
      } else {
        String libDirStr = cmdLine.getOptionValue("L");
        Path libDir = libDirStr != null ? Paths.get(libDirStr) : null;

        String sourceDirStr = cmdLine.getOptionValue("s");
        Path sourceDir = Paths.get(sourceDirStr == null ? System.getProperty("user.dir") : sourceDirStr);

        String cacheDirStr = cmdLine.getOptionValue("c");
        Path cacheDir = sourceDir.resolve(cacheDirStr != null ? cacheDirStr : ".cache");

        boolean recompile = cmdLine.hasOption("recompile");

        new ConsoleMain(libDir, sourceDir, cacheDir, recompile).run(sourceDir, cmdLine.getArgList());
      }
    } catch (ParseException e) {
      System.err.println(e.getMessage());
    }
  }

  private static void printHelp() {
    new HelpFormatter().printHelp("vclang [FILES]", cmdOptions);
  }
}
