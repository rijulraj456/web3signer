/*
 * Copyright 2023 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.web3signer.core.service.jsonrpc.handlers.internalresponse;

import static tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError.INVALID_PARAMS;
import static tech.pegasys.web3signer.core.service.jsonrpc.response.JsonRpcError.SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT;
import static tech.pegasys.web3signer.core.util.EthMessageUtil.getEthereumMessage;
import static tech.pegasys.web3signer.signing.util.IdentifierUtils.normaliseIdentifier;

import tech.pegasys.web3signer.core.service.http.handlers.signing.SignerForIdentifier;
import tech.pegasys.web3signer.core.service.jsonrpc.JsonRpcRequest;
import tech.pegasys.web3signer.core.service.jsonrpc.exceptions.JsonRpcException;
import tech.pegasys.web3signer.core.service.jsonrpc.handlers.ResultProvider;
import tech.pegasys.web3signer.signing.SecpArtifactSignature;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class EthSignResultProvider implements ResultProvider<String> {

  private static final Logger LOG = LogManager.getLogger();

  private final SignerForIdentifier<SecpArtifactSignature> transactionSignerProvider;

  public EthSignResultProvider(
      final SignerForIdentifier<SecpArtifactSignature> transactionSignerProvider) {
    this.transactionSignerProvider = transactionSignerProvider;
  }

  @Override
  public String createResponseResult(final JsonRpcRequest request) {
    final List<String> params = getParams(request);
    if (params == null || params.size() != 2) {
      LOG.debug(
          "eth_sign should have a list of 2 parameters, but has {}",
          params == null ? "null" : params.size());
      throw new JsonRpcException(INVALID_PARAMS);
    }

    final String identifier = params.get(0);
    final Bytes ethMessage = getEthereumMessage(params.get(1));
    return transactionSignerProvider
        .sign(normaliseIdentifier(identifier), ethMessage)
        .orElseThrow(
            () -> {
              LOG.debug("Address ({}) does not match any available account", identifier);
              return new JsonRpcException(SIGNING_FROM_IS_NOT_AN_UNLOCKED_ACCOUNT);
            });
  }

  private List<String> getParams(final JsonRpcRequest request) {
    try {
      @SuppressWarnings("unchecked")
      final List<String> params = (List<String>) request.getParams();
      return params;
    } catch (final ClassCastException e) {
      LOG.debug(
          "eth_sign should have a list of 2 parameters, but received an object: {}",
          request.getParams());
      throw new JsonRpcException(INVALID_PARAMS);
    }
  }
}
