package org.meveo.api.dto.response.storage;

import org.meveo.api.dto.response.BaseResponse;
import org.meveo.api.storage.RepositoryDto;

/**
 * @author Edward P. Legaspi
 */
public class RepositoryResponseDto extends BaseResponse {

	private static final long serialVersionUID = -1836370787551589107L;
	
	private RepositoryDto repository;

	public RepositoryDto getRepository() {
		return repository;
	}

	public void setRepository(RepositoryDto repository) {
		this.repository = repository;
	}
}
