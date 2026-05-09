import {JwtPayload} from "jwt-decode";

export interface CustomJwtPayload extends JwtPayload {
  exp?: number;
  roles?: string[];
}