package org.repositoryminer.checkstyle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.repositoryminer.checkstyle.logger.RepositoryMinerAudit;
import org.repositoryminer.exceptions.RepositoryMinerException;

import com.google.common.io.Closeables;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.ModuleFactory;
import com.puppycrawl.tools.checkstyle.PackageObjectFactory;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.RootModule;

public class CheckstyleExecutor {

	private String propertiesFile;
	private String configFile;
	private String repository;
	
	public void setPropertiesFile(String propertiesFile) {
		this.propertiesFile = propertiesFile;
	}

	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}

	public void setRepository(String repository) {
		this.repository = repository;
	}

	public void execute() throws CheckstyleException {
		final Properties properties;

		if (propertiesFile == null) {
			properties = System.getProperties();
		} else {
			properties = loadProperties(new File(propertiesFile));
		}

		// create configurations
		final Configuration config = ConfigurationLoader.loadConfiguration(configFile,
				new PropertiesExpander(properties));

		// create our custom audit listener
		final RepositoryMinerAudit listener = new RepositoryMinerAudit();

		final ClassLoader moduleClassLoader = Checker.class.getClassLoader();
		final RootModule rootModule = getRootModule(config.getName(), moduleClassLoader);

		rootModule.setModuleClassLoader(moduleClassLoader);
		rootModule.configure(config);
		rootModule.addListener(listener);

		// executes checkstyle
		rootModule.process(getFiles(repository));

		rootModule.destroy();
	}

	private static Properties loadProperties(File file) {
		final Properties properties = new Properties();
		FileInputStream fis = null;

		try {
			fis = new FileInputStream(file);
			properties.load(fis);
		} catch (IOException e) {
			throw new RepositoryMinerException(String.format("Can not load properties from %s", file.getAbsolutePath()),
					e);
		} finally {
			Closeables.closeQuietly(fis);
		}

		return properties;
	}

	private static RootModule getRootModule(String name, ClassLoader moduleClassLoader) throws CheckstyleException {
		final ModuleFactory factory = new PackageObjectFactory(Checker.class.getPackage().getName() + ".",
				moduleClassLoader);
		return (RootModule) factory.createModule(name);
	}

	private static List<File> getFiles(String dir) {
		Collection<File> files = FileUtils.listFiles(new File(dir), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
		return new ArrayList<File>(files);
	}

}