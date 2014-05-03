package org.greenscape.portal.setup;

import java.util.Date;
import java.util.List;

import org.greenscape.core.service.Service;
import org.greenscape.organization.OrganizationEntity;
import org.greenscape.site.Page;
import org.greenscape.site.Site;
import org.greenscape.site.SiteTemplate;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component
public class DatabaseSetup {
	private static final String GREENSCAPE = "Greenscape";
	private Service service;

	@Activate
	public void initData() {
		OrganizationEntity organization = initDefaultOrganization();
		initDefaultSite(organization);
	}

	public OrganizationEntity initDefaultOrganization() {
		List<OrganizationEntity> list = service.find(OrganizationEntity.class);
		OrganizationEntity organization = null;
		if (list == null || list.size() == 0) {
			organization = new OrganizationEntity();
			organization.setName(GREENSCAPE);
			organization.setActive(true);
			organization.setHomeURL("greenscape");
			organization.setMaxUsers(4000);
			Date now = new Date();
			organization.setCreateDate(now);
			organization.setModifiedDate(now);
			service.save(organization);
		} else {
			organization = list.get(0);
		}
		return organization;
	}

	public Site initDefaultSite(OrganizationEntity organization) {
		List<Site> list = service.find(Site.class, Site.IS_DEFAULT, true);
		Site entity = null;
		if (list == null || list.size() == 0) {
			entity = new Site();
			entity.setName(GREENSCAPE);
			entity.setHomeURL(GREENSCAPE);
			entity.setActive(true);
			entity.setDefault(true);
			entity.setOrganizationId(organization.getModelId());
			Date now = new Date();
			entity.setCreateDate(now);
			entity.setModifiedDate(now);

			SiteTemplate template = new SiteTemplate();
			template.setURL("/common/templates/main.html");
			template.setHeaderURL("/common/templates/header.html");
			template.setFooterURL("/common/templates/footer.html");
			template.setCreateDate(now);
			template.setModifiedDate(now);
			entity.setSiteTemplate(template);
			service.save(entity);
			initDefaultPage(entity);
		} else {
			entity = list.get(0);
		}
		return entity;
	}

	private void initDefaultPage(Site entity) {
		Page page = new Page();
		page.setActive(true);
		Date now = new Date();
		page.setCreateDate(now);
		page.setLayoutURL("/common/layout/2-col.html");
		page.setModifiedDate(now);
		page.setName("Home");
		page.setOrganizationId(entity.getOrganizationId());
		page.setPathURL("home");
		page.setSiteId(entity.getModelId());
		service.save(page);
		entity.getPages().add(page);
	}

	@Reference(policy = ReferencePolicy.DYNAMIC)
	public void setService(Service service) {
		this.service = service;
	}

	public void unsetService(Service service) {
		this.service = service;
	}
}
