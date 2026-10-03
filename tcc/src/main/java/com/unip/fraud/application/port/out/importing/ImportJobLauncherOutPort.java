package com.unip.fraud.application.port.out.importing;

import com.unip.fraud.application.domain.ImportJob;

public interface ImportJobLauncherOutPort {
  void launch(final ImportJob importJob);

  void restart(final ImportJob importJob);
}
