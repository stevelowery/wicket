/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wicket.ng.request.mapper;

import org.apache.wicket.Request;
import org.apache.wicket.ng.request.IRequestHandler;
import org.apache.wicket.ng.request.Url;
import org.apache.wicket.ng.request.component.PageParametersNg;
import org.apache.wicket.ng.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.ng.request.handler.resource.ResourceRequestHandler;
import org.apache.wicket.ng.request.mapper.parameters.IPageParametersEncoder;
import org.apache.wicket.ng.request.mapper.parameters.SimplePageParametersEncoder;
import org.apache.wicket.ng.resource.IResource;
import org.apache.wicket.ng.resource.ResourceReference;
import org.apache.wicket.util.lang.Classes;

/**
 * Generic {@link ResourceReference} encoder that encodes and decodes non-mounted
 * {@link ResourceReference}s.
 * <p>
 * Decodes and encodes the following URLs:
 * 
 * <pre>
 *    /wicket/resource/org.apache.wicket.ResourceScope/name
 *    /wicket/resource/org.apache.wicket.ResourceScope/name?en
 *    /wicket/resource/org.apache.wicket.ResourceScope/name?-style
 *    /wicket/resource/org.apache.wicket.ResourceScope/resource/name.xyz?en_EN-style
 * </pre>
 * 
 * @author Matej Knopp
 */
public class ResourceReferenceMapper extends AbstractResourceReferenceMapper
{
	private final IPageParametersEncoder pageParametersEncoder;

	/**
	 * Construct.
	 * 
	 * @param pageParametersEncoder
	 */
	public ResourceReferenceMapper(IPageParametersEncoder pageParametersEncoder)
	{
		this.pageParametersEncoder = pageParametersEncoder;
	}

	/**
	 * Construct.
	 */
	public ResourceReferenceMapper()
	{
		this(new SimplePageParametersEncoder());
	}

	/**
	 * @see org.apache.wicket.ng.request.IRequestMapper#mapRequest(org.apache.wicket.Request)
	 */
	public IRequestHandler mapRequest(Request request)
	{
		Url url = request.getUrl();
		if (url.getSegments().size() >= 4 &&
			urlStartsWith(url, getContext().getNamespace(), getContext().getResourceIdentifier()))
		{
			String className = url.getSegments().get(2);
			StringBuilder name = new StringBuilder();
			for (int i = 3; i < url.getSegments().size(); ++i)
			{
				if (name.length() > 0)
				{
					name.append("/");
				}
				name.append(url.getSegments().get(i));
			}

			ResourceReferenceAttributes attributes = getResourceReferenceAttributes(url);

			// extract the PageParameters from URL if there are any
			PageParametersNg pageParameters = extractPageParameters(request, url.getSegments()
				.size(), pageParametersEncoder);

			Class<?> scope = resolveClass(className);
			if (scope != null)
			{
				ResourceReference res = getContext().getResourceReferenceRegistry()
					.getResourceReference(scope, name.toString(), attributes.locale,
						attributes.style, attributes.variation, false);
				if (res != null)
				{
					IResource resource = res.getResource();
					if (resource != null)
					{
						ResourceRequestHandler handler = new ResourceRequestHandler(resource,
							attributes.locale, attributes.style, attributes.variation,
							pageParameters);
						return handler;
					}
				}
			}
		}
		return null;
	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	protected Class<?> resolveClass(String name)
	{
		return Classes.resolveClass(name);
	}

	/**
	 * 
	 * @param scope
	 * @return
	 */
	protected String getClassName(Class<?> scope)
	{
		return scope.getName();
	}

	/**
	 * @see org.apache.wicket.ng.request.IRequestMapper#mapHandler(org.apache.wicket.ng.request.IRequestHandler)
	 */
	public Url mapHandler(IRequestHandler requestHandler)
	{
		if (requestHandler instanceof ResourceReferenceRequestHandler)
		{
			ResourceReferenceRequestHandler referenceRequestHandler = (ResourceReferenceRequestHandler)requestHandler;
			ResourceReference reference = referenceRequestHandler.getResourceReference();
			Url url = new Url();
			url.getSegments().add(getContext().getNamespace());
			url.getSegments().add(getContext().getResourceIdentifier());
			url.getSegments().add(getClassName(reference.getScope()));
			String nameParts[] = reference.getName().split("/");
			for (String name : nameParts)
			{
				url.getSegments().add(name);
			}
			encodeResourceReferenceAttributes(url, reference);
			PageParametersNg parameters = referenceRequestHandler.getPageParameters();
			if (parameters != null)
			{
				parameters = new PageParametersNg(parameters);
				// need to remove indexed parameters otherwise the URL won't be able to decode
				parameters.clearIndexedParameters();
				url = encodePageParameters(url, parameters, pageParametersEncoder);
			}
			return url;
		}
		return null;
	}

	/**
	 * @see org.apache.wicket.ng.request.IRequestMapper#getCompatibilityScore(org.apache.wicket.Request)
	 */
	public int getCompatibilityScore(Request request)
	{
		// always return 0 here so that the mounts have higher priority
		return 0;
	}
}