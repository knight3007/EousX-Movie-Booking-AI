import { Controller, Get } from '@nestjs/common';

@Controller()
export class AppController {
  @Get('health')
  getHealth() {
    return {
      status: 'ok',
      app: 'EousX API Server',
      timestamp: new Date().toISOString(),
    };
  }
}