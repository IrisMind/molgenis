package org.molgenis.data.annotation.resources.impl;

import org.molgenis.data.Repository;

import java.io.File;
import java.io.IOException;

public interface RepositoryFactory
{
	Repository createRepository(File file) throws IOException;
	Repository createRepository(File file, Integer redisDBIndex) throws IOException;
}
