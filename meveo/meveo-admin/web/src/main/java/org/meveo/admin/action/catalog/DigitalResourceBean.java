package org.meveo.admin.action.catalog;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.meveo.admin.action.BaseBean;
import org.meveo.model.catalog.DigitalResource;
import org.meveo.service.base.local.IPersistenceService;
import org.meveo.service.catalog.impl.DigitalResourceService;

/**
 * @author Edward P. Legaspi
 */
@Named
@ViewScoped
public class DigitalResourceBean extends BaseBean<DigitalResource> {

	private static final long serialVersionUID = -7239556219857397001L;

	@Inject
	private DigitalResourceService digitalResourceService;
	
	public DigitalResourceBean() {
		super(DigitalResource.class);
	}

	@Override
	protected IPersistenceService<DigitalResource> getPersistenceService() {
		return digitalResourceService;
	}

}
