package org.greenscape.portal.setup;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.greenscape.core.Action;
import org.greenscape.core.ModelResource;
import org.greenscape.core.Resource;
import org.greenscape.core.ResourceRegistry;
import org.greenscape.core.model.Organization;
import org.greenscape.core.model.OrganizationModel;
import org.greenscape.core.model.Page;
import org.greenscape.core.model.PageModel;
import org.greenscape.core.model.Permission;
import org.greenscape.core.model.PermissionModel;
import org.greenscape.core.model.RoleEntity;
import org.greenscape.core.model.RoleModel;
import org.greenscape.core.model.Site;
import org.greenscape.core.model.SiteModel;
import org.greenscape.core.model.SiteTemplate;
import org.greenscape.core.model.UserEntity;
import org.greenscape.core.model.UserModel;
import org.greenscape.core.service.Service;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.log.LogService;

@Component
public class DatabaseSetup {
	private static final String GREENSCAPE = "Greenscape";
	private ResourceRegistry resourceRegistry;
	private Service service;

	private LogService logService;

	@Activate
	public void activate() {
		logService.log(LogService.LOG_INFO, "Starting Greenscape setup");
		Organization organization = initDefaultOrganization();
		Site site = initDefaultSite(organization);
		initDefaultUsers(organization, site);
		logService.log(LogService.LOG_INFO, "Greenscape setup completed");
	}

	public Organization initDefaultOrganization() {
		ModelResource modelResource = (ModelResource) resourceRegistry.getResource(OrganizationModel.MODEL_NAME);
		Organization organization = null;
		if (modelResource != null) {
			List<Organization> list = service.find(OrganizationModel.MODEL_NAME);
			if (list == null || list.size() == 0) {
				organization = new Organization();
				organization.setName(GREENSCAPE);
				organization.setActive(true);
				organization.setHomeURL("greenscape");
				organization.setMaxUsers(4000);
				Date now = new Date();
				organization.setCreatedDate(now);
				organization.setModifiedDate(now);
				service.save(OrganizationModel.MODEL_NAME, organization);
			} else {
				organization = list.get(0);
			}
		}
		return organization;
	}

	public Site initDefaultSite(Organization organization) {
		if (organization == null) {
			return null;
		}
		List<Site> list = service.find(organization.getModelId(), SiteModel.MODEL_NAME, Site.IS_DEFAULT, true);
		Site site = null;
		if (list == null || list.size() == 0) {
			site = new Site();
			site.setName(GREENSCAPE);
			site.setHomeURL(GREENSCAPE);
			site.setActive(true);
			site.setDefault(true);
			site.setOrganizationId(organization.getModelId());
			Date now = new Date();
			site.setCreatedDate(now);
			site.setModifiedDate(now);

			SiteTemplate template = new SiteTemplate();
			template.setURL("/common/templates/main.html");
			template.setHeaderURL("/common/templates/header.html");
			template.setFooterURL("/common/templates/footer.html");
			template.setCreatedDate(now);
			template.setModifiedDate(now);
			template.setOrganizationId(organization.getModelId());
			site.setSiteTemplate(template);
			service.save(SiteModel.MODEL_NAME, site);
			initDefaultPage(site);
		} else {
			site = list.get(0);
		}
		return site;
	}

	private void initDefaultPage(Site site) {
		Page page = new Page();
		page.setActive(true);
		Date now = new Date();
		page.setCreatedDate(now);
		page.setLayoutURL("/common/layout/2-col.html");
		page.setModifiedDate(now);
		page.setName("Home");
		page.setOrganizationId(site.getOrganizationId());
		page.setPathURL("home");
		page.setSiteId(site.getModelId());
		service.save(PageModel.MODEL_NAME, page);
		site.getPages().add(page);
		// TODO: add web content weblet with welcome message
	}

	private void initDefaultUsers(Organization organization, Site site) {
		if (organization == null || site == null) {
			return;
		}
		// init admin role and user
		List<RoleEntity> roles = service.find(organization.getModelId(), RoleModel.MODEL_NAME, RoleEntity.NAME,
				"Administrator");
		RoleEntity adminRole = null;
		Date now = new Date();

		if (roles == null || roles.isEmpty()) {
			adminRole = new RoleEntity();
			adminRole.setCreatedDate(now).setModifiedDate(now);
			adminRole.setName("Administrator").setDescription("The administrator of the entire application")
			.setOrganizationId(organization.getModelId());
			service.save(RoleModel.MODEL_NAME, adminRole);
		} else {
			adminRole = roles.get(0);
		}
		// TODO: add permissions to role

		List<UserEntity> users = service.find(UserModel.MODEL_NAME, UserEntity.USER_NAME, "admin");
		if (users == null || users.isEmpty()) {
			UserEntity user = new UserEntity();
			user.setAgreedToTermsOfUse(true).setCreatedDate(now).setModifiedDate(now);
			user.setEmail("admin@greenscape.org").setEmailAddressVerified(true).setFirstName("Admin")
			.setOrganizationId(organization.getModelId());
			user.setPassword("admin").setStatus(1).setUserName("admin");
			Set<String> userRoles = new HashSet<String>();
			userRoles.add(adminRole.getModelId());
			user.setRoles(userRoles);
			service.save(UserModel.MODEL_NAME, user);
		}

		// init guest role and user
		roles = service.find(organization.getModelId(), RoleModel.MODEL_NAME, RoleEntity.NAME, "Guest");
		RoleEntity guestRole = null;

		if (roles == null || roles.isEmpty()) {
			guestRole = new RoleEntity();
			guestRole.setCreatedDate(now).setModifiedDate(now);
			guestRole.setName("Guest").setDescription("The default guest user")
			.setOrganizationId(organization.getModelId());
			service.save(RoleModel.MODEL_NAME, guestRole);
			List<Resource> resources = resourceRegistry.getResources();
			for (Resource resource : resources) {
				List<Action> guestActions = resource.getPermission().getGuestDefaults();
				if (guestActions != null && !guestActions.isEmpty()) {
					Long actionIds = guestActions.get(0).getBit();
					Permission permission = new Permission();
					permission.setName(resource.getName());
					permission.setOrganizationId(organization.getModelId());
					permission.setRoleId(guestRole.getModelId());
					permission.setScope(1);
					for (Action action : guestActions) {
						actionIds = actionIds | action.getBit();
						permission.setActionIds(actionIds);
					}
					service.save(PermissionModel.MODEL_NAME, permission);
				}
			}
		} else {
			guestRole = roles.get(0);
		}

		users = service.find(UserModel.MODEL_NAME, UserEntity.USER_NAME, "guest");
		if (users == null || users.isEmpty()) {
			UserEntity user = new UserEntity();
			user.setAgreedToTermsOfUse(false).setCreatedDate(now).setModifiedDate(now);
			user.setEmail("guest@greenscape.org").setEmailAddressVerified(true).setFirstName("Guest")
			.setOrganizationId(organization.getModelId());
			user.setPassword("guest").setStatus(1).setUserName("guest");
			Set<String> userRoles = new HashSet<String>();
			userRoles.add(guestRole.getModelId());
			user.setRoles(userRoles);
			service.save(UserModel.MODEL_NAME, user);
		}
	}

	@Reference(policy = ReferencePolicy.DYNAMIC)
	public void setResourceRegistry(ResourceRegistry resourceRegistry) {
		this.resourceRegistry = resourceRegistry;
	}

	public void unsetResourceRegistry(ResourceRegistry resourceRegistry) {
		this.resourceRegistry = null;
	}

	@Reference(policy = ReferencePolicy.DYNAMIC)
	public void setService(Service service) {
		this.service = service;
	}

	public void unsetService(Service service) {
		this.service = null;
	}

	@Reference(policy = ReferencePolicy.DYNAMIC)
	public void setLogService(LogService logService) {
		this.logService = logService;
	}

	public void unsetLogService(LogService logService) {
		this.logService = null;
	}
}
