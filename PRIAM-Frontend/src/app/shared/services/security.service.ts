import {Injectable} from "@angular/core";
import {KeycloakEventType, KeycloakService} from "keycloak-angular";
import {KeycloakProfile} from "keycloak-js";

@Injectable({providedIn: "root"})
export class SecurityService {
  public profile?: KeycloakProfile;
  public idReference?: unknown;

  constructor(public kcService: KeycloakService) {
    this.init();
  }

  init() {
    this.kcService.keycloakEvents$.subscribe({
      next: (e) => {
        if (e.type === KeycloakEventType.OnAuthSuccess) {
          this.kcService.loadUserProfile().then(profile => {
            this.profile = profile;
            console.log('User Profile:', this.profile);
            /*
                        if (this.profile.attributes && this.profile.attributes['idReference']) {
                          this.idReference = this.profile.attributes['idReference'];
                        } else {
                          console.log('idReference non trouvé dans les attributs du profil');
                        }*/
          });
        }
      }
    });
  }

  getIdReference(): number | null {
    let referenceId: number | null = null;

    if (this.profile && this.profile.attributes && Array.isArray(this.profile.attributes['idReference'])) {
      const idReferenceValue = this.profile.attributes['idReference'][0];
      if (typeof idReferenceValue === 'string') {
        const parsedId = parseInt(idReferenceValue, 10);
        if (!isNaN(parsedId)) {
          referenceId = parsedId;
        }
      }
    }

    console.log('Reference ID:', referenceId);
    return referenceId;
  }



}
