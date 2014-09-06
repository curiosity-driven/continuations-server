/**
 * Copyright 2014 Curiosity driven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.curiositydriven.continuations;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContinuationPending;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

@WebServlet("/")
public class ContinuationsServlet extends HttpServlet {

	private static final String CONTINUATION_KEY = "continuation";
	private static final String READ_METHOD_NAME = "read";

	private static final long serialVersionUID = 1L;

	private final Method readMethod;

	public ContinuationsServlet() {
		try {
			this.readMethod = ContinuationsServlet.class.getDeclaredMethod(
					READ_METHOD_NAME, String.class);
		} catch (NoSuchMethodException e) {
			throw new AssertionError("Method declared", e);
		} catch (SecurityException e) {
			throw new AssertionError("Method declared", e);
		}
	}

	public static String read(String parameter) {
		Context context = Context.enter();
		try {
			ContinuationPending pending = context.captureContinuation();
			pending.setApplicationState(parameter);
			throw pending;
		} finally {
			Context.exit();
		}
	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		this.doGet(request, response);
	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);

		HttpSession session = request.getSession(true);

		Object state = session.getAttribute(CONTINUATION_KEY);

		String scriptName = request.getRequestURI() + ".js";
		String value = request.getParameter("value");

		PrintWriter writer = response.getWriter();

		try {
			writer.println("<h1>Hello Servlet</h1>");
			writer.println("<form method=post>");
			Result result = executeScript(scriptName, state, value);
			if (result.isDone()) {
				writer.println("<h2>Result</h2>");
				writer.println(result.value);
				session.removeAttribute(CONTINUATION_KEY);
			} else {
				writer.println("<h2>" + result.value + "</h2>");
				writer.println("<input autofocus name=value>");
				writer.println("<input type=submit></form>");
				session.setAttribute(CONTINUATION_KEY, result.state);
			}
		} finally {
			writer.close();
		}
	}

	private Result executeScript(String scriptName, Object state,
			String value) throws IOException {

		Context ctx = Context.enter();
		ctx.setOptimizationLevel(-1);

		ScriptableObject scope = ctx.initStandardObjects();

		FunctionObject readFunction = new FunctionObject(READ_METHOD_NAME,
				readMethod, scope);
		scope.put(READ_METHOD_NAME, scope, readFunction);

		Script script = ctx.compileReader(getScriptReader(scriptName),
				scriptName, 1, null);

		try {
			Object result;
			if (state == null) {
				result = ctx.executeScriptWithContinuations(script, scope);
			} else {
				result = ctx.resumeContinuation(state, scope, value);
			}
			return Result.done(result);
		} catch (ContinuationPending pending) {
			return Result.resume(pending.getApplicationState(),
					pending.getContinuation());
		} finally {
			Context.exit();
		}
	}

	private InputStreamReader getScriptReader(String scriptName) {
		return new InputStreamReader(
				ContinuationsServlet.class.getResourceAsStream(scriptName));
	}

	private static final class Result {
		public final Object value;

		public final Object state;

		private Result(Object value, Object state) {
			this.value = value;
			this.state = state;
		}

		public boolean isDone() {
			return this.state == null;
		}

		public static Result done(Object result) {
			return new Result(result, null);
		}

		public static Result resume(Object value, Object state) {
			return new Result(value, state);
		}
	}
}