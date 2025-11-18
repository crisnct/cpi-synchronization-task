package com.figaf.training.cpisync.infrastructure.cpi;

import com.figaf.integration.common.entity.CloudPlatformType;
import com.figaf.integration.common.entity.ConnectionProperties;
import com.figaf.integration.common.entity.Platform;
import com.figaf.integration.common.entity.RequestContext;
import com.figaf.integration.common.entity.WebApiAccessMode;

public class RequestContextFactory {

    public static RequestContext createRequestContextForWebApi(CpiSystemConnectionParameters parameters) {
        ConnectionProperties connectionProperties = new ConnectionProperties();
        connectionProperties.setHost(parameters.host());
        connectionProperties.setPort("443");
        connectionProperties.setProtocol("https");
        connectionProperties.setUsername(parameters.username());
        connectionProperties.setPassword(parameters.password());

        RequestContext requestContext = new RequestContext();
        requestContext.setPlatform(Platform.CPI);
        requestContext.setCloudPlatformType(CloudPlatformType.CLOUD_FOUNDRY);
        requestContext.setRestTemplateWrapperKey("testAgent");
        requestContext.setConnectionProperties(connectionProperties);
        requestContext.setWebApiAccessMode(WebApiAccessMode.SAP_IDENTITY_SERVICE);

        return requestContext;
    }
}
